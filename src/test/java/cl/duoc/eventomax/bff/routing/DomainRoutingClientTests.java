package cl.duoc.eventomax.bff.routing;

import java.net.URI;
import java.time.Duration;

import cl.duoc.eventomax.bff.LocalDownstreamServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class DomainRoutingClientTests {

	@Test
	void returnsBadGatewayWhenDownstreamCannotBeReached() throws Exception {
		URI unavailable;
		try (var downstream = LocalDownstreamServer.start()) {
			unavailable = downstream.baseUrl();
		}
		var configuration = new DomainRoutingConfiguration();
		try (var httpClient = configuration.domainHttpClient(Duration.ofMillis(500))) {
			var factory = configuration.domainRequestFactory(httpClient, Duration.ofSeconds(1));
			var client = new DomainRoutingClient(RestClient.builder().requestFactory(factory).build(), unavailable, unavailable);
			var request = new MockHttpServletRequest("GET", "/api/productions/42");
			var response = client.productions(HttpMethod.GET, request);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
			assertThat(response.getBody()).isNull();
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "/relative", "file:///tmp/service", "https://user:password@example.test",
			"https://example.test?target=other", "https://example.test#fragment" })
	void rejectsInvalidDownstreamBaseUrls(String value) {
		var client = RestClient.create();
		assertThatIllegalArgumentException().isThrownBy(() ->
				new DomainRoutingClient(client, URI.create(value), URI.create("https://example.test")));
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void readTimeoutCoversDelayedHeadersAndBody(boolean sendHeadersFirst) throws Exception {
		var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			try (exchange) {
				if (sendHeadersFirst) {
					exchange.sendResponseHeaders(200, 10);
					exchange.getResponseBody().write(1);
					exchange.getResponseBody().flush();
				}
				Thread.sleep(1500);
				if (!sendHeadersFirst) exchange.sendResponseHeaders(200, -1);
			}
			catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
			catch (java.io.IOException ex) { /* The timed-out client closes the connection. */ }
		});
		server.start();
		try {
			var configuration = new DomainRoutingConfiguration();
			try (var httpClient = configuration.domainHttpClient(Duration.ofMillis(500))) {
				assertThat(httpClient.connectTimeout()).contains(Duration.ofMillis(500));
				var factory = configuration.domainRequestFactory(httpClient, Duration.ofMillis(200));
				var uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
				var client = new DomainRoutingClient(RestClient.builder().requestFactory(factory).build(), uri, uri);
				long start = System.nanoTime();
				var response = client.productions(HttpMethod.GET, new MockHttpServletRequest("GET", "/api/productions"));
				assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
				assertThat(response.getBody()).isNull();
				assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(1));
			}
		}
		finally { server.stop(0); }
	}

}
