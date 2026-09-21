package cl.duoc.eventomax.bff.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.Assert;

public final class AudienceValidator implements OAuth2TokenValidator<Jwt> {

	private static final OAuth2Error INVALID_AUDIENCE =
			new OAuth2Error("invalid_token", "The required audience is missing", null);

	private final String audience;

	public AudienceValidator(String audience) {
		Assert.hasText(audience, "JWT audience must not be blank");
		this.audience = audience;
	}

	@Override
	public OAuth2TokenValidatorResult validate(Jwt jwt) {
		if (jwt.getAudience() != null && jwt.getAudience().contains(this.audience)) {
			return OAuth2TokenValidatorResult.success();
		}
		return OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE);
	}

}
