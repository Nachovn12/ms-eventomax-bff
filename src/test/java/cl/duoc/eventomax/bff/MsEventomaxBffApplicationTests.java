package cl.duoc.eventomax.bff;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "management.endpoints.web.exposure.include=health,info")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Import(MsEventomaxBffApplicationTests.TestEndpoint.class)
class MsEventomaxBffApplicationTests {

	private static final LocalJwtIssuer ISSUER = LocalJwtIssuer.start();
	private static final LocalDownstreamServer PRODUCTIONS = LocalDownstreamServer.start();
	private static final LocalDownstreamServer CATALOG = LocalDownstreamServer.start();

	@Autowired
	private MockMvc mockMvc;

	@DynamicPropertySource
	static void jwtProperties(DynamicPropertyRegistry registry) {
		registry.add("ENTRA_ISSUER_URI", ISSUER::issuer);
		registry.add("ENTRA_AUDIENCE", () -> LocalJwtIssuer.AUDIENCE);
		registry.add("PRODUCTIONS_BASE_URL", PRODUCTIONS::baseUrl);
		registry.add("CATALOG_BASE_URL", CATALOG::baseUrl);
	}

	@BeforeEach
	void resetDownstreams() {
		PRODUCTIONS.reset();
		CATALOG.reset();
	}

	@AfterAll
	static void stopIssuer() {
		ISSUER.close();
		PRODUCTIONS.close();
		CATALOG.close();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void acceptsValidJwt() throws Exception {
		String token = ISSUER.sign(ISSUER.validClaims());

		this.mockMvc.perform(get("/test/protected").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(content().string("authenticated"));
	}

	@Test
	void rejectsWrongAudience() throws Exception {
		assertRejected(ISSUER.sign(ISSUER.validClaims().audience("another-api")));
	}

	@Test
	void rejectsWrongIssuer() throws Exception {
		assertRejected(ISSUER.sign(ISSUER.validClaims().issuer("https://untrusted.example.test")));
	}

	@Test
	void rejectsExpiredToken() throws Exception {
		Instant now = Instant.now();
		assertRejected(ISSUER.sign(ISSUER.validClaims()
				.issueTime(Date.from(now.minusSeconds(900)))
				.notBeforeTime(Date.from(now.minusSeconds(900)))
				.expirationTime(Date.from(now.minusSeconds(300)))));
	}

	@Test
	void rejectsTokenBeforeNotBefore() throws Exception {
		Instant now = Instant.now();
		assertRejected(ISSUER.sign(ISSUER.validClaims()
				.notBeforeTime(Date.from(now.plusSeconds(300)))
				.expirationTime(Date.from(now.plusSeconds(600)))));
	}

	@Test
	void rejectsInvalidSignature() throws Exception {
		assertRejected(ISSUER.signWithUntrustedKey(ISSUER.validClaims()));
	}

	@Test
	void rejectsMissingAudience() throws Exception {
		assertRejected(ISSUER.sign(ISSUER.validClaims().claim("aud", null)));
	}

	@Test
	void rejectsMalformedToken() throws Exception {
		assertRejected("not-a-jwt");
	}

	@ParameterizedTest
	@ValueSource(strings = { "/actuator/health", "/actuator/info" })
	void permitsPublicActuatorEndpoints(String path) throws Exception {
		this.mockMvc.perform(get(path)).andExpect(status().isOk());
	}

	@ParameterizedTest
	@ValueSource(strings = { "/test/protected", "/actuator/env", "/actuator/health/private", "/unknown" })
	void requiresAuthenticationForOtherPaths(String path) throws Exception {
		this.mockMvc.perform(get(path))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")));
	}

	@Test
	void acceptsBearerPostWithoutCsrfAndDoesNotCreateSession() throws Exception {
		String token = ISSUER.sign(ISSUER.validClaims());

		var result = this.mockMvc.perform(post("/test/protected")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
				.andReturn();

		assertThat(result.getRequest().getSession(false)).isNull();
		this.mockMvc.perform(get("/test/protected")).andExpect(status().isUnauthorized());
	}

	@Test
	void rejectsUnauthenticatedPost() throws Exception {
		this.mockMvc.perform(post("/test/protected")).andExpect(status().isUnauthorized());
	}

	@Test
	void doesNotExposeSessionLogout() throws Exception {
		this.mockMvc.perform(post("/logout")).andExpect(status().isUnauthorized());
	}

	private void assertRejected(String token) throws Exception {
		this.mockMvc.perform(get("/test/protected").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")));
	}

	@ParameterizedTest(name = "{0} {1} with {2} -> {3}")
	@CsvSource(textBlock = """
			GET,  /api/productions/42,        Admin,       200
			GET,  /api/productions/42,        Productor,   200
			GET,  /api/productions/42,        Organizador, 200
			GET,  /api/productions/42,        Auditor,     403
			POST, /api/productions,           Admin,       403
			POST, /api/productions,           Productor,   200
			POST, /api/productions,           Organizador, 200
			POST, /api/productions,           Auditor,     403
			PUT,  /api/productions/42/status, Admin,       200
			PUT,  /api/productions/42/status, Productor,   200
			PUT,  /api/productions/42/status, Organizador, 403
			PUT,  /api/productions/42/status, Auditor,     403
			GET,  /api/catalog/services,      Admin,       200
			GET,  /api/catalog/services,      Productor,   200
			GET,  /api/catalog/services,      Organizador, 403
			GET,  /api/catalog/services,      Auditor,     403
			POST, /api/catalog/services,      Admin,       200
			POST, /api/catalog/services,      Productor,   403
			POST, /api/catalog/services,      Organizador, 403
			POST, /api/catalog/services,      Auditor,     403
			PUT,  /api/catalog/services/42,   Admin,       200
			PUT,  /api/catalog/services/42,   Productor,   403
			PUT,  /api/catalog/services/42,   Organizador, 403
			PUT,  /api/catalog/services/42,   Auditor,     403
			GET,  /api/report/summary,        Admin,       404
			GET,  /api/report/summary,        Productor,   403
			GET,  /api/report/summary,        Organizador, 403
			GET,  /api/report/summary,        Auditor,     403
			GET,  /api/audit/events,          Admin,       404
			GET,  /api/audit/events,          Productor,   403
			GET,  /api/audit/events,          Organizador, 403
			GET,  /api/audit/events,          Auditor,     404
			""")
	void enforcesRoleMatrix(String method, String path, String role, int expectedStatus) throws Exception {
		String token = ISSUER.sign(ISSUER.validClaims("access_as_user", role));

		var result = this.mockMvc.perform(request(HttpMethod.valueOf(method), path)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().is(expectedStatus));
		if (expectedStatus == 200) {
			result.andExpect(content().string("authenticated"));
			assertThat(downstreamRequestCount()).isEqualTo(1);
		}
		else if (expectedStatus == 403) {
			result.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE,
					containsString("error=\"insufficient_scope\"")));
		}
		if (expectedStatus != 200) {
			assertThat(downstreamRequestCount()).isZero();
		}
	}

	@ParameterizedTest
	@MethodSource("domainRequests")
	void requiresAccessAsUserEvenWithCorrectRole(String method, String path, String role) throws Exception {
		assertForbidden(method, path, ISSUER.sign(ISSUER.validClaims(null, role)));
		assertForbidden(method, path, ISSUER.sign(ISSUER.validClaims("another_scope", role)));
	}

	@ParameterizedTest
	@MethodSource("domainRequests")
	void requiresRoleEvenWithCorrectScope(String method, String path, String role) throws Exception {
		assertForbidden(method, path, ISSUER.sign(ISSUER.validClaims("access_as_user").claim("roles", null)));
		assertForbidden(method, path,
				ISSUER.sign(ISSUER.validClaims("access_as_user", role.toLowerCase(Locale.ROOT))));
	}

	@ParameterizedTest
	@MethodSource("domainRequests")
	void rejectsMissingOrInvalidBearerOnDomainRoutes(String method, String path, String role) throws Exception {
		this.mockMvc.perform(request(HttpMethod.valueOf(method), path))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")));

		String token = ISSUER.signWithUntrustedKey(ISSUER.validClaims("access_as_user", role));
		this.mockMvc.perform(request(HttpMethod.valueOf(method), path)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("error=\"invalid_token\"")));
		assertThat(downstreamRequestCount()).isZero();
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
			GET, /api/productions, 200
			GET, /api/catalog, 404
			GET, /api/report, 404
			GET, /api/audit, 404
			GET, /api/productions/42/details, 404
			PUT, /api/catalog/services/42/details, 404
			""")
	void appliesWildcardRulesToRootAndNestedPaths(String method, String path, int expectedStatus) throws Exception {
		String token = ISSUER.sign(ISSUER.validClaims("access_as_user", "Admin"));
		this.mockMvc.perform(request(HttpMethod.valueOf(method), path)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().is(expectedStatus));
		assertThat(downstreamRequestCount()).isEqualTo(expectedStatus == 200 ? 1 : 0);

		assertForbidden(method, path, ISSUER.sign(ISSUER.validClaims()));
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
			POST, /api/productions/42, 405
			PUT, /api/productions/42/details, 404
			PUT, /api/productions/42/extra/status, 404
			DELETE, /api/catalog/services/42, 405
			POST, /api/report/summary, 404
			POST, /api/audit/events, 404
			""")
	void unspecifiedRoutesOnlyRequireAuthentication(String method, String path, int expectedStatus) throws Exception {
		this.mockMvc.perform(request(HttpMethod.valueOf(method), path)).andExpect(status().isUnauthorized());

		String token = ISSUER.sign(ISSUER.validClaims());
		this.mockMvc.perform(request(HttpMethod.valueOf(method), path)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().is(expectedStatus));
		assertThat(downstreamRequestCount()).isZero();
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
			POST, /actuator/health
			POST, /actuator/info
			HEAD, /actuator/health
			HEAD, /actuator/info
			""")
	void onlyGetIsPublicForActuator(String method, String path) throws Exception {
		this.mockMvc.perform(request(HttpMethod.valueOf(method), path)).andExpect(status().isUnauthorized());
	}

	@Test
	void acceptsAnyAllowedRoleAlongsideOtherRolesAndScopes() throws Exception {
		String token = ISSUER.sign(ISSUER.validClaims("another_scope access_as_user", "Auditor", "Productor"));
		this.mockMvc.perform(post("/api/productions").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk());
	}

	private void assertForbidden(String method, String path, String token) throws Exception {
		int before = downstreamRequestCount();
		this.mockMvc.perform(request(HttpMethod.valueOf(method), path)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isForbidden())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("error=\"insufficient_scope\"")));
		assertThat(downstreamRequestCount()).isEqualTo(before);
	}

	private static int downstreamRequestCount() {
		return PRODUCTIONS.requests().size() + CATALOG.requests().size();
	}

	@ParameterizedTest(name = "routes {0} {1} -> {3}")
	@CsvSource(textBlock = """
			POST, /api/productions, Organizador, 201
			GET, /api/productions/42, Productor, 200
			GET, /api/productions, Productor, 200
			PUT, /api/productions/42/status, Productor, 202
			GET, /api/catalog/services, Productor, 200
			POST, /api/catalog/services, Admin, 201
			PUT, /api/catalog/services/service-42, Admin, 200
			""")
	void routesApprovedContracts(String method, String path, String role, int downstreamStatus) throws Exception {
		boolean productions = path.startsWith("/api/productions");
		LocalDownstreamServer downstream = productions ? PRODUCTIONS : CATALOG;
		LocalDownstreamServer other = productions ? CATALOG : PRODUCTIONS;
		byte[] response = "{ \"id\": \"42\", \"description\": \"Respuesta íntegra\" }\n".getBytes(StandardCharsets.UTF_8);
		String contentType = "application/json;charset=UTF-8";
		String location = path + "/created-42";
		downstream.respond(downstreamStatus, response, downstreamStatus == 201
				? Map.of("Content-Type", contentType, "Location", location)
				: Map.of("Content-Type", contentType));
		byte[] body = method.equals("GET") ? new byte[0]
				: "{ \"value\": \"payload opaco ñ\" }\n".getBytes(StandardCharsets.UTF_8);
		String token = ISSUER.sign(ISSUER.validClaims("access_as_user", role));
		var outgoing = request(HttpMethod.valueOf(method), path)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.header(HttpHeaders.ACCEPT, "application/json")
				.content(body);
		if (body.length > 0) {
			outgoing.contentType(contentType);
		}
		var result = this.mockMvc.perform(outgoing)
				.andExpect(status().is(downstreamStatus))
				.andExpect(content().bytes(response))
				.andExpect(header().string(HttpHeaders.CONTENT_TYPE, contentType));
		if (downstreamStatus == 201) {
			result.andExpect(header().string(HttpHeaders.LOCATION, location));
		}
		assertThat(downstream.requests()).hasSize(1);
		assertThat(other.requests()).isEmpty();
		var captured = downstream.requests().getFirst();
		assertThat(captured.method()).isEqualTo(method);
		assertThat(captured.path()).isEqualTo(path);
		assertThat(captured.body()).isEqualTo(body);
		assertThat(captured.headers().getFirst(HttpHeaders.ACCEPT)).isEqualTo("application/json");
		assertThat(("Bearer " + token).equals(captured.headers().getFirst(HttpHeaders.AUTHORIZATION)))
				.as("Original Bearer token preserved without printing it").isTrue();
		if (body.length > 0) {
			assertThat(captured.headers().getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo(contentType);
		}
	}

	@Test
	void preservesRawQueryIncludingRepeatedParametersAndEncoding() throws Exception {
		String query = "status=CONFIRMADO&from=2026-09-01&to=2026-09-30"
				+ "&tag=a%2Bb&other=first&tag=a+b&empty=&flag&filter=%7B%22a%22%3A1%7D";
		String token = ISSUER.sign(ISSUER.validClaims("access_as_user", "Productor"));
		this.mockMvc.perform(get(URI.create("/api/productions?" + query))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andExpect(status().isOk());

		assertThat(PRODUCTIONS.requests()).hasSize(1);
		assertThat(PRODUCTIONS.requests().getFirst().query()).isEqualTo(query);
	}

	@Test
	void preservesEncodedPathWithoutDoubleEncoding() throws Exception {
		String path = "/api/productions/evento-%C3%B1";
		String token = ISSUER.sign(ISSUER.validClaims("access_as_user", "Productor"));
		this.mockMvc.perform(get(URI.create(path)).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk());

		assertThat(PRODUCTIONS.requests().getFirst().path()).isEqualTo(path);
	}

	@ParameterizedTest
	@ValueSource(ints = { 400, 401, 403, 404, 409, 500 })
	void preservesDownstreamErrors(int downstreamStatus) throws Exception {
		byte[] body = "{\"error\":\"respuesta del dominio\"}".getBytes(StandardCharsets.UTF_8);
		PRODUCTIONS.respond(downstreamStatus, body, Map.of("Content-Type", "application/problem+json"));
		String token = ISSUER.sign(ISSUER.validClaims("access_as_user", "Organizador"));
		this.mockMvc.perform(post("/api/productions").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().is(downstreamStatus))
				.andExpect(content().bytes(body))
				.andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/problem+json"));
		assertThat(PRODUCTIONS.requests()).hasSize(1);
	}

	@Test
	void doesNotFollowDownstreamRedirects() throws Exception {
		String location = CATALOG.baseUrl() + "/must-not-receive-bearer";
		PRODUCTIONS.respond(302, new byte[0], Map.of("Location", location));
		String token = ISSUER.sign(ISSUER.validClaims("access_as_user", "Productor"));
		this.mockMvc.perform(get("/api/productions/42").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isFound())
				.andExpect(header().string(HttpHeaders.LOCATION, location));
		assertThat(PRODUCTIONS.requests()).hasSize(1);
		assertThat(CATALOG.requests()).isEmpty();
	}

	@Test
	void forwardsOpaqueBodyWithoutDomainOrJsonValidation() throws Exception {
		byte[] body = new byte[] { 0, 1, 2, (byte) 255, '{' };
		String token = ISSUER.sign(ISSUER.validClaims("access_as_user", "Admin"));
		this.mockMvc.perform(post("/api/catalog/services").contentType("application/json").content(body)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andExpect(status().isOk());
		assertThat(CATALOG.requests().getFirst().body()).isEqualTo(body);
	}

	@Test
	void onlyCopiesApprovedHeaders() throws Exception {
		PRODUCTIONS.respond(200, new byte[] { 1, 2, 3 }, Map.of(
				"Content-Type", "application/octet-stream", "Connection", "close",
				"Set-Cookie", "test-cookie=value", "X-Internal", "downstream-only"));
		String token = ISSUER.sign(ISSUER.validClaims("access_as_user", "Organizador"));
		this.mockMvc.perform(post("/api/productions")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.header(HttpHeaders.HOST, "untrusted.example.test")
				.header(HttpHeaders.CONNECTION, "close")
				.header(HttpHeaders.CONTENT_LENGTH, "999")
				.header(HttpHeaders.COOKIE, "client-cookie=value")
				.header("X-Internal", "client-only")
				.header(HttpHeaders.ACCEPT, "application/json", "application/octet-stream")
				.content(new byte[] { 1, 2, 3 }))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist(HttpHeaders.CONNECTION))
				.andExpect(header().doesNotExist(HttpHeaders.TRANSFER_ENCODING))
				.andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
				.andExpect(header().doesNotExist("X-Internal"));

		var captured = PRODUCTIONS.requests().getFirst();
		assertThat(captured.headers().getFirst(HttpHeaders.HOST)).isEqualTo(PRODUCTIONS.baseUrl().getAuthority());
		assertThat(captured.headers().getFirst(HttpHeaders.CONNECTION)).isNotEqualTo("close");
		assertThat(captured.headers().getFirst(HttpHeaders.CONTENT_LENGTH)).isNotEqualTo("999");
		assertThat(captured.headers().getFirst(HttpHeaders.COOKIE)).isNull();
		assertThat(captured.headers().getFirst("X-Internal")).isNull();
		assertThat(captured.headers().getAccept()).hasSize(2);
	}

	@Test
	void preservesNoContentResponse() throws Exception {
		CATALOG.respond(204, new byte[0], Map.of());
		String token = ISSUER.sign(ISSUER.validClaims("access_as_user", "Admin"));
		this.mockMvc.perform(request(HttpMethod.PUT, "/api/catalog/services/42")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isNoContent()).andExpect(content().bytes(new byte[0]));
	}

	@ParameterizedTest
	@ValueSource(strings = { "/api/productions", "/api/productions/42", "/api/catalog/services" })
	void doesNotRouteImplicitHeadRequests(String path) throws Exception {
		String token = ISSUER.sign(ISSUER.validClaims());
		this.mockMvc.perform(request(HttpMethod.HEAD, path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isMethodNotAllowed());
		assertThat(downstreamRequestCount()).isZero();
	}

	private static Stream<Arguments> domainRequests() {
		return Stream.of(
				Arguments.of("GET", "/api/productions/42", "Productor"),
				Arguments.of("POST", "/api/productions", "Organizador"),
				Arguments.of("PUT", "/api/productions/42/status", "Productor"),
				Arguments.of("GET", "/api/catalog/services", "Productor"),
				Arguments.of("POST", "/api/catalog/services", "Admin"),
				Arguments.of("PUT", "/api/catalog/services/42", "Admin"),
				Arguments.of("GET", "/api/report/summary", "Admin"),
				Arguments.of("GET", "/api/audit/events", "Auditor"));
	}

	@RestController
	static class TestEndpoint {

		@RequestMapping(path = "/test/protected", method = { RequestMethod.GET, RequestMethod.POST })
		String protectedEndpoint() {
			return "authenticated";
		}

	}

}
