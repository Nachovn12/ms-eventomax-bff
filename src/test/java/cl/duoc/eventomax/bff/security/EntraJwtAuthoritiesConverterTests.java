package cl.duoc.eventomax.bff.security;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

class EntraJwtAuthoritiesConverterTests {

	private final EntraJwtAuthoritiesConverter converter = new EntraJwtAuthoritiesConverter();

	@Test
	void mapsAdminRole() {
		Jwt jwt = token().claim("roles", List.of("Admin")).build();

		assertThat(authorities(jwt)).containsExactly("ROLE_Admin");
	}

	@Test
	void mapsMultipleRoles() {
		Jwt jwt = token().claim("roles", List.of("Producer", "Organizer")).build();

		assertThat(authorities(jwt)).containsExactlyInAnyOrder("ROLE_Producer", "ROLE_Organizer");
	}

	@Test
	void preservesScopesWhenAddingRoles() {
		Jwt jwt = token().claim("scp", "access_as_user another_scope")
				.claim("roles", List.of("Admin", "Auditor")).build();

		assertThat(authorities(jwt)).containsExactlyInAnyOrder(
				"SCOPE_access_as_user", "SCOPE_another_scope", "ROLE_Admin", "ROLE_Auditor");
	}

	@ParameterizedTest
	@ValueSource(strings = { "scp", "scope" })
	void preservesStandardScopeClaimsWithoutRoles(String claim) {
		Jwt jwt = token().claim(claim, "access_as_user").build();

		assertThat(authorities(jwt)).containsExactly("SCOPE_access_as_user");
	}

	@Test
	void preservesStandardScopeClaimPrecedence() {
		Jwt jwt = token().claim("scope", "standard_scope").claim("scp", "access_as_user")
				.claim("roles", List.of("Admin")).build();

		assertThat(authorities(jwt)).containsExactlyInAnyOrder("SCOPE_standard_scope", "ROLE_Admin");
	}

	@Test
	void handlesMissingClaims() {
		assertThat(authorities(token().build())).isEmpty();
	}

	@Test
	void removesDuplicateAuthorities() {
		Jwt jwt = token().claim("scp", "access_as_user access_as_user")
				.claim("roles", List.of("Admin", "Admin")).build();

		assertThat(authorities(jwt)).containsExactlyInAnyOrder("SCOPE_access_as_user", "ROLE_Admin");
	}

	@Test
	void preservesRoleCase() {
		Jwt jwt = token().claim("roles", List.of("admin")).build();

		assertThat(authorities(jwt)).containsExactly("ROLE_admin").doesNotContain("ROLE_Admin");
	}

	private List<String> authorities(Jwt jwt) {
		return this.converter.convert(jwt).stream().map(GrantedAuthority::getAuthority).toList();
	}

	private Jwt.Builder token() {
		return Jwt.withTokenValue("unit-test-token").header("alg", "RS256").subject("test-user");
	}

}
