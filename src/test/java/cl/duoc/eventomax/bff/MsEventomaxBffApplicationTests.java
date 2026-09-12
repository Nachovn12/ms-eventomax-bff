package cl.duoc.eventomax.bff;

import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
@AutoConfigureMockMvc
@Import(MsEventomaxBffApplicationTests.TestEndpoint.class)
class MsEventomaxBffApplicationTests {

	private static final LocalJwtIssuer ISSUER = LocalJwtIssuer.start();

	@Autowired
	private MockMvc mockMvc;

	@DynamicPropertySource
	static void jwtProperties(DynamicPropertyRegistry registry) {
		registry.add("ENTRA_ISSUER_URI", ISSUER::issuer);
		registry.add("ENTRA_AUDIENCE", () -> LocalJwtIssuer.AUDIENCE);
	}

	@AfterAll
	static void stopIssuer() {
		ISSUER.close();
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
			GET,  /api/catalog/items,         Admin,       200
			GET,  /api/catalog/items,         Productor,   200
			GET,  /api/catalog/items,         Organizador, 403
			GET,  /api/catalog/items,         Auditor,     403
			POST, /api/catalog/items,         Admin,       200
			POST, /api/catalog/items,         Productor,   403
			POST, /api/catalog/items,         Organizador, 403
			POST, /api/catalog/items,         Auditor,     403
			PUT,  /api/catalog/items/42,      Admin,       200
			PUT,  /api/catalog/items/42,      Productor,   403
			PUT,  /api/catalog/items/42,      Organizador, 403
			PUT,  /api/catalog/items/42,      Auditor,     403
			GET,  /api/report/summary,        Admin,       200
			GET,  /api/report/summary,        Productor,   403
			GET,  /api/report/summary,        Organizador, 403
			GET,  /api/report/summary,        Auditor,     403
			GET,  /api/audit/events,          Admin,       200
			GET,  /api/audit/events,          Productor,   403
			GET,  /api/audit/events,          Organizador, 403
			GET,  /api/audit/events,          Auditor,     200
			""")
	void enforcesRoleMatrix(String method, String path, String role, int expectedStatus) throws Exception {
		String token = ISSUER.sign(ISSUER.validClaims("access_as_user", role));

		var result = this.mockMvc.perform(request(HttpMethod.valueOf(method), path)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().is(expectedStatus));
		if (expectedStatus == 200) {
			result.andExpect(content().string("authenticated"));
		}
		else {
			result.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE,
					containsString("error=\"insufficient_scope\"")));
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
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
			GET, /api/productions
			GET, /api/catalog
			GET, /api/report
			GET, /api/audit
			GET, /api/productions/42/details
			PUT, /api/catalog/items/42/details
			""")
	void appliesWildcardRulesToRootAndNestedPaths(String method, String path) throws Exception {
		String token = ISSUER.sign(ISSUER.validClaims("access_as_user", "Admin"));
		this.mockMvc.perform(request(HttpMethod.valueOf(method), path)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk());

		assertForbidden(method, path, ISSUER.sign(ISSUER.validClaims()));
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
			POST, /api/productions/42
			PUT, /api/productions/42/details
			PUT, /api/productions/42/extra/status
			DELETE, /api/catalog/items/42
			POST, /api/report/summary
			POST, /api/audit/events
			""")
	void unspecifiedRoutesOnlyRequireAuthentication(String method, String path) throws Exception {
		this.mockMvc.perform(request(HttpMethod.valueOf(method), path)).andExpect(status().isUnauthorized());

		String token = ISSUER.sign(ISSUER.validClaims());
		this.mockMvc.perform(request(HttpMethod.valueOf(method), path)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk());
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
		this.mockMvc.perform(request(HttpMethod.valueOf(method), path)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isForbidden())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("error=\"insufficient_scope\"")));
	}

	private static Stream<Arguments> domainRequests() {
		return Stream.of(
				Arguments.of("GET", "/api/productions/42", "Productor"),
				Arguments.of("POST", "/api/productions", "Organizador"),
				Arguments.of("PUT", "/api/productions/42/status", "Productor"),
				Arguments.of("GET", "/api/catalog/items", "Productor"),
				Arguments.of("POST", "/api/catalog/items", "Admin"),
				Arguments.of("PUT", "/api/catalog/items/42", "Admin"),
				Arguments.of("GET", "/api/report/summary", "Admin"),
				Arguments.of("GET", "/api/audit/events", "Auditor"));
	}

	@RestController
	static class TestEndpoint {

		@RequestMapping(path = "/test/protected", method = { RequestMethod.GET, RequestMethod.POST })
		String protectedEndpoint() {
			return "authenticated";
		}

		@RequestMapping("/api/**")
		String domainEndpoint() {
			return "authenticated";
		}

	}

}
