package cl.duoc.eventomax.bff;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

final class LocalJwtIssuer implements AutoCloseable {

	static final String AUDIENCE = "eventomax-test-api";

	private final HttpServer server;

	private final RSAKey signingKey;

	private LocalJwtIssuer() throws IOException, JOSEException {
		this.signingKey = new RSAKeyGenerator(2048).keyID("test-key").generate();
		this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		this.server.createContext("/.well-known/openid-configuration", exchange -> respond(exchange,
				"{\"issuer\":\"" + issuer() + "\",\"jwks_uri\":\"" + issuer() + "/jwks\"}"));
		this.server.createContext("/jwks", exchange -> respond(exchange,
				new JWKSet(this.signingKey.toPublicJWK()).toString()));
		this.server.start();
	}

	static LocalJwtIssuer start() {
		try {
			return new LocalJwtIssuer();
		}
		catch (IOException | JOSEException ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}

	String issuer() {
		return "http://127.0.0.1:" + this.server.getAddress().getPort();
	}

	JWTClaimsSet.Builder validClaims() {
		Instant now = Instant.now();
		return new JWTClaimsSet.Builder()
				.issuer(issuer())
				.audience(AUDIENCE)
				.subject("test-user")
				.issueTime(Date.from(now.minusSeconds(30)))
				.notBeforeTime(Date.from(now.minusSeconds(30)))
				.expirationTime(Date.from(now.plusSeconds(300)));
	}

	String sign(JWTClaimsSet.Builder claims) throws JOSEException {
		return sign(claims, this.signingKey);
	}

	String signWithUntrustedKey(JWTClaimsSet.Builder claims) throws JOSEException {
		return sign(claims, new RSAKeyGenerator(2048).keyID("test-key").generate());
	}

	private String sign(JWTClaimsSet.Builder claims, RSAKey key) throws JOSEException {
		var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
				.type(JOSEObjectType.JWT).keyID(key.getKeyID()).build(), claims.build());
		jwt.sign(new RSASSASigner(key));
		return jwt.serialize();
	}

	private void respond(HttpExchange exchange, String json) throws IOException {
		try (exchange) {
			byte[] body = json.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
		}
	}

	@Override
	public void close() {
		this.server.stop(0);
	}

}
