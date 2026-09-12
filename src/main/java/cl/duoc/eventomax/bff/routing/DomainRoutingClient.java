package cl.duoc.eventomax.bff.routing;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

public class DomainRoutingClient {

	private static final List<String> REQUEST_HEADERS =
			List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT);
	private static final List<String> RESPONSE_HEADERS =
			List.of(HttpHeaders.CONTENT_TYPE, HttpHeaders.LOCATION);

	private final RestClient client;
	private final String productionsBaseUrl;
	private final String catalogBaseUrl;

	public DomainRoutingClient(RestClient client, URI productionsBaseUrl, URI catalogBaseUrl) {
		this.client = client;
		this.productionsBaseUrl = baseUrl(productionsBaseUrl);
		this.catalogBaseUrl = baseUrl(catalogBaseUrl);
	}

	ResponseEntity<byte[]> productions(HttpMethod method, HttpServletRequest request) throws IOException {
		return forward(this.productionsBaseUrl, method, request);
	}

	ResponseEntity<byte[]> catalog(HttpMethod method, HttpServletRequest request) throws IOException {
		return forward(this.catalogBaseUrl, method, request);
	}

	private ResponseEntity<byte[]> forward(String baseUrl, HttpMethod method, HttpServletRequest incoming)
			throws IOException {
		// MVC also matches HEAD to GET mappings; only the approved method may reach the downstream.
		if (!method.matches(incoming.getMethod())) {
			return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
		}
		String path = incoming.getRequestURI().substring(incoming.getContextPath().length());
		String query = incoming.getQueryString();
		// Servlet path/query are already encoded. Keep raw order, repeated parameters and escapes.
		// The configured authority stays fixed; never resolve an incoming URL against the base.
		URI target = URI.create(baseUrl + path + (query == null ? "" : "?" + query));
		byte[] body = incoming.getInputStream().readAllBytes();
		var outgoing = this.client.method(method).uri(target).headers(headers -> {
			for (String name : REQUEST_HEADERS) {
				var values = Collections.list(incoming.getHeaders(name));
				if (!values.isEmpty()) {
					headers.put(name, values);
				}
			}
		});
		if (body.length > 0) {
			outgoing.body(output -> output.write(body));
		}
		try {
			return outgoing.exchange((request, response) -> {
				var headers = new HttpHeaders();
				for (String name : RESPONSE_HEADERS) {
					List<String> values = response.getHeaders().get(name);
					if (values != null) {
						headers.put(name, values);
					}
				}
				return new ResponseEntity<>(response.getBody().readAllBytes(), headers, response.getStatusCode());
			});
		}
		catch (ResourceAccessException ex) {
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
		}
	}

	private static String baseUrl(URI uri) {
		Assert.isTrue(uri != null && ("http".equalsIgnoreCase(uri.getScheme())
				|| "https".equalsIgnoreCase(uri.getScheme())) && uri.getHost() != null,
				"Downstream base URL must be an absolute HTTP(S) URL");
		Assert.isTrue(uri.getRawUserInfo() == null && uri.getRawQuery() == null && uri.getRawFragment() == null,
				"Downstream base URL must not contain credentials, query or fragment");
		return uri.toASCIIString().replaceAll("/+$", "");
	}

}
