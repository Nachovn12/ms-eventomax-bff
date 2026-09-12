package cl.duoc.eventomax.bff;

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

	@RestController
	static class TestEndpoint {

		@RequestMapping(path = "/test/protected", method = { RequestMethod.GET, RequestMethod.POST })
		String protectedEndpoint() {
			return "authenticated";
		}

	}

}
