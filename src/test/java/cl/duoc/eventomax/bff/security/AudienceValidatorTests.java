package cl.duoc.eventomax.bff.security;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class AudienceValidatorTests {

	private final AudienceValidator validator = new AudienceValidator("eventomax-test-api");

	@Test
	void acceptsExpectedAudienceAmongMultipleAudiences() {
		Jwt jwt = token().audience(List.of("another-api", "eventomax-test-api")).build();

		assertThat(this.validator.validate(jwt).hasErrors()).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = { "another-api", "eventomax-test-api-suffix", "EVENTOMAX-TEST-API" })
	void rejectsAudienceThatDoesNotMatchExactly(String audience) {
		Jwt jwt = token().audience(List.of(audience)).build();

		assertThat(this.validator.validate(jwt).getErrors())
				.singleElement().satisfies(error -> assertThat(error.getErrorCode()).isEqualTo("invalid_token"));
	}

	@Test
	void rejectsMissingAudience() {
		assertThat(this.validator.validate(token().build()).hasErrors()).isTrue();
	}

	@Test
	void rejectsEmptyAudience() {
		Jwt jwt = token().audience(List.of()).build();

		assertThat(this.validator.validate(jwt).hasErrors()).isTrue();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = " ")
	void rejectsMissingOrBlankConfiguredAudience(String audience) {
		assertThatIllegalArgumentException().isThrownBy(() -> new AudienceValidator(audience));
	}

	private Jwt.Builder token() {
		return Jwt.withTokenValue("unit-test-token").header("alg", "RS256").subject("test-user");
	}

}
