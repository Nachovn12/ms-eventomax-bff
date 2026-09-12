package cl.duoc.eventomax.bff.security;

import java.util.Collection;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.DelegatingJwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

public final class EntraJwtAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

	private final DelegatingJwtGrantedAuthoritiesConverter delegate;

	public EntraJwtAuthoritiesConverter() {
		var scopes = new JwtGrantedAuthoritiesConverter();
		var roles = new JwtGrantedAuthoritiesConverter();
		roles.setAuthoritiesClaimName("roles");
		roles.setAuthorityPrefix("ROLE_");
		this.delegate = new DelegatingJwtGrantedAuthoritiesConverter(scopes, roles);
	}

	@Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		return this.delegate.convert(jwt);
	}

}
