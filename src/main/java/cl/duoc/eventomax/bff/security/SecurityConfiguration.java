package cl.duoc.eventomax.bff.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.util.Assert;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

	private static final String ADMIN = "Admin";
	private static final String PRODUCER = "Productor";
	private static final String ORGANIZER = "Organizador";
	private static final String AUDITOR = "Auditor";

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http,
			JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
		var entryPoint = new BearerTokenAuthenticationEntryPoint();
		var deniedHandler = new BearerTokenAccessDeniedHandler();

		return http
				// Authentication uses the Bearer header, without session cookies.
				.csrf(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/productions/**")
								.access(scopeAndAnyRole(ADMIN, PRODUCER, ORGANIZER))
						.requestMatchers(HttpMethod.POST, "/api/productions")
								.access(scopeAndAnyRole(PRODUCER, ORGANIZER))
						.requestMatchers(HttpMethod.PUT, "/api/productions/{id}/status")
								.access(scopeAndAnyRole(ADMIN, PRODUCER))
						.requestMatchers(HttpMethod.GET, "/api/catalog/**")
								.access(scopeAndAnyRole(ADMIN, PRODUCER))
						.requestMatchers(HttpMethod.POST, "/api/catalog/**")
								.access(scopeAndAnyRole(ADMIN))
						.requestMatchers(HttpMethod.PUT, "/api/catalog/**")
								.access(scopeAndAnyRole(ADMIN))
						.requestMatchers(HttpMethod.GET, "/api/report/**")
								.access(scopeAndAnyRole(ADMIN))
						.requestMatchers(HttpMethod.GET, "/api/audit/**")
								.access(scopeAndAnyRole(ADMIN, AUDITOR))
						.anyRequest().authenticated())
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(entryPoint)
						.accessDeniedHandler(deniedHandler))
				.oauth2ResourceServer(resourceServer -> resourceServer
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
						.authenticationEntryPoint(entryPoint)
						.accessDeniedHandler(deniedHandler))
				.build();
	}

	private static AuthorizationManager<RequestAuthorizationContext> scopeAndAnyRole(String... roles) {
		return AuthorizationManagers.allOf(
				AuthorityAuthorizationManager.hasAuthority("SCOPE_access_as_user"),
				AuthorityAuthorizationManager.hasAnyRole(roles));
	}

	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		var converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(new EntraJwtAuthoritiesConverter());
		return converter;
	}

	@Bean
	JwtDecoder jwtDecoder(
			@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
			@Value("${eventomax.security.jwt.audience}") String audience) {
		Assert.hasText(issuer, "JWT issuer must not be blank");
		var validator = new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefaultWithIssuer(issuer), new AudienceValidator(audience));

		// Preserve deferred discovery: public health checks do not require Entra ID.
		return new SupplierJwtDecoder(() -> {
			NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuer).build();
			decoder.setJwtValidator(validator);
			return decoder;
		});
	}

}
