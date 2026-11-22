package cl.duoc.eventomax.bff;

import java.nio.charset.StandardCharsets;
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
 * Pruebas de seguridad y enrutamiento transparente del BFF para POST /api/audit/actor-aliases/me.
 * Utiliza dobles HTTP locales (NO constituye integración contra Audit desplegado en AWS).
 */
@SpringBootTest(properties = "management.endpoints.web.exposure.include=health,info")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class AuditActorAliasRoutingTests {

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

	@ParameterizedTest(name = "POST /api/audit/actor-aliases/me with {0} + access_as_user -> 204")
	@ValueSource(strings = { "Admin", "Productor", "Organizador", "Auditor" })
	void allFourEventoMaxRolesCanRegisterOwnActorAliasAndPreserveAuthAndTracingHeaders(String role) throws Exception {
		AUDIT.respond(204, new byte[0], Map.of());
		String token = ISSUER.sign(ISSUER.validClaims("access_as_user", role));

		this.mockMvc.perform(post("/api/audit/actor-aliases/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.header("X-Trace-Id", "trace-alias-123")
				.header("X-Correlation-Id", "corr-alias-456"))
				.andExpect(status().isNoContent())
				.andExpect(content().bytes(new byte[0]));

		assertThat(AUDIT.requests()).hasSize(1);
		var captured = AUDIT.requests().getFirst();
		assertThat(captured.method()).isEqualTo("POST");
		assertThat(captured.path()).isEqualTo("/api/audit/actor-aliases/me");
		assertThat(captured.headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + token);
		assertThat(captured.headers().getFirst("X-Trace-Id")).isEqualTo("trace-alias-123");
		assertThat(captured.headers().getFirst("X-Correlation-Id")).isEqualTo("corr-alias-456");
	}

	@Test
	void rejectsMissingTokenMissingScopeAndUnauthorizedRole() throws Exception {
		// Sin token -> 401
		this.mockMvc.perform(post("/api/audit/actor-aliases/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")));

		// Sin scope -> 403
		String noScope = ISSUER.sign(ISSUER.validClaims(null, "Productor"));
		this.mockMvc.perform(post("/api/audit/actor-aliases/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + noScope))
				.andExpect(status().isForbidden())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("insufficient_scope")));

		// Sin rol autorizado -> 403
		String noRole = ISSUER.sign(ISSUER.validClaims("access_as_user"));
		this.mockMvc.perform(post("/api/audit/actor-aliases/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + noRole))
				.andExpect(status().isForbidden());

		String unknownRole = ISSUER.sign(ISSUER.validClaims("access_as_user", "ExternalGuest"));
		this.mockMvc.perform(post("/api/audit/actor-aliases/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + unknownRole))
				.andExpect(status().isForbidden());

		assertThat(AUDIT.requests()).isEmpty();
	}

	@ParameterizedTest(name = "GET /api/audit/timeline with {0} -> {1}")
	@CsvSource(textBlock = """
			Admin,       200
			Auditor,     200
			Productor,   403
			Organizador, 403
			""")
	void preservesAuditTimelineReadAuthorizationMatrix(String role, int expectedStatus) throws Exception {
		AUDIT.respond(200, "[]".getBytes(StandardCharsets.UTF_8),
				Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
		String token = ISSUER.sign(ISSUER.validClaims("access_as_user", role));

		this.mockMvc.perform(get("/api/audit/timeline")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().is(expectedStatus));

		this.mockMvc.perform(get("/api/audit/timeline/latest")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().is(expectedStatus));

		assertThat(AUDIT.requests()).hasSize(expectedStatus == 200 ? 2 : 0);
	}

	@Test
	void propagates400OnNonEmptyBodyAnd409OnContradictoryBindingFromAudit() throws Exception {
		String token = ISSUER.sign(ISSUER.validClaims("access_as_user", "Productor"));

		// 400 Bad Request desde Audit (body no vacío)
		String err400 = "{\"error\":\"Bad Request\",\"message\":\"Cuerpo no permitido\"}";
		AUDIT.respond(400, err400.getBytes(StandardCharsets.UTF_8),
				Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

		this.mockMvc.perform(post("/api/audit/actor-aliases/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"sub\":\"spoofed\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().json(err400));

		// 409 Conflict desde Audit (asociación contradictoria)
		String err409 = "{\"error\":\"Conflict\",\"message\":\"Asociación contradictoria\"}";
		AUDIT.respond(409, err409.getBytes(StandardCharsets.UTF_8),
				Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

		this.mockMvc.perform(post("/api/audit/actor-aliases/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isConflict())
				.andExpect(content().json(err409));

		assertThat(AUDIT.requests()).hasSize(2);
	}

	@Test
	void rejectsDisallowedHttpMethodsAndOtherPostAuditRoutes() throws Exception {
		String adminToken = ISSUER.sign(ISSUER.validClaims("access_as_user", "Admin"));
		String producerToken = ISSUER.sign(ISSUER.validClaims("access_as_user", "Productor"));

		// PUT / DELETE on /api/audit/actor-aliases/me -> 403 via denyAll()
		this.mockMvc.perform(put("/api/audit/actor-aliases/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
				.andExpect(status().isForbidden());
		this.mockMvc.perform(delete("/api/audit/actor-aliases/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
				.andExpect(status().isForbidden());

		// GET on /api/audit/actor-aliases/me -> 403 for Productor, 405 for Admin (never forwarded to Audit)
		this.mockMvc.perform(get("/api/audit/actor-aliases/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + producerToken))
				.andExpect(status().isForbidden());
		this.mockMvc.perform(get("/api/audit/actor-aliases/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
				.andExpect(status().isMethodNotAllowed());

		// Other POST under /api/audit/** -> 403 via denyAll()
		this.mockMvc.perform(post("/api/audit/timeline")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
				.andExpect(status().isForbidden());

		assertThat(AUDIT.requests()).isEmpty();
	}
}
