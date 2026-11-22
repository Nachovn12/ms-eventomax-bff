package cl.duoc.eventomax.bff;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de seguridad y enrutamiento transparente del BFF para:
 * - GET /api/organizers/eligible
 * - PUT /api/organizers/{organizerId}/contact (incluyendo autocontacto de Productor)
 * - Reenvío transparente de POST /api/productions en modalidades SELF y EXTERNAL
 *
 * NOTA: Estas pruebas validan la capa BFF; la comprobación de propiedad (tid:oid)
 * para Productor sin Admin se aplica en ms-eventomax-productions.
 */
@SpringBootTest(properties = "management.endpoints.web.exposure.include=health,info")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class OrganizersEligibleRoutingTests {

	private static final LocalJwtIssuer ISSUER = LocalJwtIssuer.start();
	private static final LocalDownstreamServer PRODUCTIONS = LocalDownstreamServer.start();
	private static final LocalDownstreamServer CATALOG = LocalDownstreamServer.start();
	private static final LocalDownstreamServer REPORT = LocalDownstreamServer.start();
	private static final LocalDownstreamServer AUDIT = LocalDownstreamServer.start();

	@Autowired
	private MockMvc mockMvc;

	@DynamicPropertySource
	static void properties(DynamicPropertyRegistry registry) {
		registry.add("ENTRA_ISSUER_URI", ISSUER::issuer);
		registry.add("ENTRA_AUDIENCE", () -> LocalJwtIssuer.AUDIENCE);
		registry.add("PRODUCTIONS_BASE_URL", PRODUCTIONS::baseUrl);
		registry.add("CATALOG_BASE_URL", CATALOG::baseUrl);
		registry.add("REPORT_BASE_URL", REPORT::baseUrl);
		registry.add("AUDIT_BASE_URL", AUDIT::baseUrl);
		registry.add("KAFKA_ADMIN_BASE_URL", PRODUCTIONS::baseUrl);
	}

	@BeforeEach
	void resetDownstreams() {
		PRODUCTIONS.reset();
		CATALOG.reset();
		REPORT.reset();
		AUDIT.reset();
	}

	@AfterAll
	static void stopServers() {
		ISSUER.close();
		PRODUCTIONS.close();
		CATALOG.close();
		REPORT.close();
		AUDIT.close();
	}

	// --- GET /api/organizers/eligible ---

	@ParameterizedTest(name = "GET /api/organizers/eligible with {0} -> {1}")
	@CsvSource(textBlock = """
			Admin,       200
			Productor,   200
			Organizador, 403
			Auditor,     403
			""")
	void eligibleEndpointEnforcesRoleMatrix(String role, int expectedStatus) throws Exception {
		String token = ISSUER.sign(ISSUER.validClaims("access_as_user", role));

		this.mockMvc.perform(get("/api/organizers/eligible")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().is(expectedStatus));

		assertThat(PRODUCTIONS.requests()).hasSize(expectedStatus == 200 ? 1 : 0);
	}

	@Test
	void eligibleEndpointAllowsAdminPlusProductor() throws Exception {
		String token = ISSUER.sign(ISSUER.validClaims("access_as_user", "Admin", "Productor"));

		this.mockMvc.perform(get("/api/organizers/eligible")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk());

		assertThat(PRODUCTIONS.requests()).hasSize(1);
	}

	@Test
	void eligibleEndpointRejectsMissingExpiredUntrustedAndNoScopeJwt() throws Exception {
		// Missing JWT -> 401
		this.mockMvc.perform(get("/api/organizers/eligible"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")));

		// Expired JWT -> 401
		Instant now = Instant.now();
		String expired = ISSUER.sign(ISSUER.validClaims("access_as_user", "Productor")
				.issueTime(Date.from(now.minusSeconds(900)))
				.notBeforeTime(Date.from(now.minusSeconds(900)))
				.expirationTime(Date.from(now.minusSeconds(300))));
		this.mockMvc.perform(get("/api/organizers/eligible")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
				.andExpect(status().isUnauthorized());

		// Untrusted signature -> 401
		String untrusted = ISSUER.signWithUntrustedKey(ISSUER.validClaims("access_as_user", "Admin"));
		this.mockMvc.perform(get("/api/organizers/eligible")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + untrusted))
				.andExpect(status().isUnauthorized());

		// Missing scope -> 403
		String noScope = ISSUER.sign(ISSUER.validClaims(null, "Productor"));
		this.mockMvc.perform(get("/api/organizers/eligible")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + noScope))
				.andExpect(status().isForbidden())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("insufficient_scope")));

		assertThat(PRODUCTIONS.requests()).isEmpty();
	}

	@Test
	void eligibleEndpointPreservesQueryEncodingRepeatedParamsAuthHeaderAndJsonPayload() throws Exception {
		String responseJson = "{\"items\":[{\"organizerId\":\"ext-1\",\"displayName\":\"Sur\",\"source\":\"EXTERNAL_CONTACT\",\"hasRegisteredContact\":true,\"maskedEmail\":\"co***@sur.cl\"}],\"page\":1,\"size\":15,\"total\":1,\"directorySyncStatus\":\"DISABLED\"}";
		PRODUCTIONS.respond(200, responseJson.getBytes(StandardCharsets.UTF_8),
				Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

		String token = ISSUER.sign(ISSUER.validClaims("access_as_user", "Productor"));
		URI rawUri = URI.create("/api/organizers/eligible?q=Producci%C3%B3n%20Sur&tag=a&tag=b&page=1&size=15");

		this.mockMvc.perform(get(rawUri)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(content().json(responseJson));

		assertThat(PRODUCTIONS.requests()).hasSize(1);
		var forwarded = PRODUCTIONS.requests().getFirst();
		assertThat(forwarded.method()).isEqualTo("GET");
		assertThat(forwarded.path()).isEqualTo("/api/organizers/eligible");
		assertThat(forwarded.query()).isEqualTo("q=Producci%C3%B3n%20Sur&tag=a&tag=b&page=1&size=15");
		assertThat(forwarded.headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + token);
	}

	@ParameterizedTest
	@ValueSource(ints = { 200, 400, 401, 403, 404, 503 })
	void eligibleEndpointPreservesDownstreamStatusCodes(int downstreamStatus) throws Exception {
		String body = "{\"status\":" + downstreamStatus + ",\"message\":\"downstream\"}";
		PRODUCTIONS.respond(downstreamStatus, body.getBytes(StandardCharsets.UTF_8),
				Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
		String token = ISSUER.sign(ISSUER.validClaims("access_as_user", "Admin"));

		this.mockMvc.perform(get("/api/organizers/eligible")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().is(downstreamStatus))
				.andExpect(content().json(body));
	}

	@Test
	void eligibleEndpointRejectsUnsupportedMethodsWithoutCallingDownstream() throws Exception {
		String token = ISSUER.sign(ISSUER.validClaims("access_as_user", "Admin", "Productor"));

		this.mockMvc.perform(post("/api/organizers/eligible")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isMethodNotAllowed());

		this.mockMvc.perform(delete("/api/organizers/eligible")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isMethodNotAllowed());

		assertThat(PRODUCTIONS.requests()).isEmpty();
	}

	// --- PUT /api/organizers/{organizerId}/contact (autocontacto de Productor) ---

	@ParameterizedTest(name = "PUT /api/organizers/tid:oid/contact with {0} -> {1}")
	@CsvSource(textBlock = """
			Admin,       200
			Productor,   200
			Organizador, 200
			Auditor,     403
			""")
	void putContactEnforcesRoleMatrixAndForwardsBodyAndAuthorization(String role, int expectedStatus) throws Exception {
		String payload = "{\"email\":\"productor.propio@duocuc.cl\",\"contactName\":\"Productor Propio\"}";
		PRODUCTIONS.respond(200, payload.getBytes(StandardCharsets.UTF_8),
				Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
		String token = ISSUER.sign(ISSUER.validClaims("access_as_user", role));

		this.mockMvc.perform(put("/api/organizers/f191bdc8-7926-4722-9ec0-cba663701940:c3057bf6-9e39-4fa4-b042-d3932d49ebc0/contact")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
				.andExpect(status().is(expectedStatus));

		if (expectedStatus == 200) {
			assertThat(PRODUCTIONS.requests()).hasSize(1);
			var forwarded = PRODUCTIONS.requests().getFirst();
			assertThat(forwarded.method()).isEqualTo("PUT");
			assertThat(forwarded.headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + token);
			assertThat(new String(forwarded.body(), StandardCharsets.UTF_8)).isEqualTo(payload);
		}
		else {
			assertThat(PRODUCTIONS.requests()).isEmpty();
		}
	}

	@Test
	void putContactRejectsMissingJwtAndMissingScope() throws Exception {
		String path = "/api/organizers/tid:oid/contact";
		this.mockMvc.perform(put(path)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"a@b.cl\"}"))
				.andExpect(status().isUnauthorized());

		String noScope = ISSUER.sign(ISSUER.validClaims(null, "Productor"));
		this.mockMvc.perform(put(path)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + noScope)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"a@b.cl\"}"))
				.andExpect(status().isForbidden());

		assertThat(PRODUCTIONS.requests()).isEmpty();
	}

	// --- Regresiones SELF y EXTERNAL en POST /api/productions ---

	@Test
	void postProductionsForwardsSelfAndExternalPayloadsTransparently() throws Exception {
		String selfPayload = "{\"organizerMode\":\"SELF\",\"name\":\"Evento Propio\",\"scheduledAt\":\"2035-06-15T18:00:00\",\"location\":\"Santiago\"}";
		String externalPayload = "{\"organizerMode\":\"EXTERNAL\",\"externalOrganizerType\":\"NEW_EXTERNAL\",\"externalClientName\":\"Cliente Sur\",\"externalClientEmail\":\"contacto@sur.cl\",\"name\":\"Festival\",\"scheduledAt\":\"2035-07-20T20:00:00\",\"location\":\"Concepcion\"}";
		PRODUCTIONS.respond(201, "{\"id\":10}".getBytes(StandardCharsets.UTF_8),
				Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

		String producerToken = ISSUER.sign(ISSUER.validClaims("access_as_user", "Admin", "Productor"));

		this.mockMvc.perform(post("/api/productions")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + producerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(selfPayload))
				.andExpect(status().isCreated());

		this.mockMvc.perform(post("/api/productions")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + producerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(externalPayload))
				.andExpect(status().isCreated());

		assertThat(PRODUCTIONS.requests()).hasSize(2);
		assertThat(new String(PRODUCTIONS.requests().get(0).body(), StandardCharsets.UTF_8)).isEqualTo(selfPayload);
		assertThat(new String(PRODUCTIONS.requests().get(1).body(), StandardCharsets.UTF_8)).isEqualTo(externalPayload);
		assertThat(PRODUCTIONS.requests().get(0).headers().getFirst(HttpHeaders.AUTHORIZATION))
				.isEqualTo("Bearer " + producerToken);
	}

}
