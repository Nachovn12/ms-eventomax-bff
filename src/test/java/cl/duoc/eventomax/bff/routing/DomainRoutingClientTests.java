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

}
