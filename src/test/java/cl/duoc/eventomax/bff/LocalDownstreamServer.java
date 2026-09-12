package cl.duoc.eventomax.bff;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.springframework.http.HttpHeaders;

public final class LocalDownstreamServer implements AutoCloseable {

	private final HttpServer server;
	private final ConcurrentLinkedQueue<CapturedRequest> requests = new ConcurrentLinkedQueue<>();
	private volatile Reply reply;

	private LocalDownstreamServer() throws IOException {
		this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		this.server.createContext("/", this::handle);
		reset();
		this.server.start();
	}

	public static LocalDownstreamServer start() {
		try {
			return new LocalDownstreamServer();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Cannot start local downstream", ex);
		}
	}

	public URI baseUrl() {
		return URI.create("http://127.0.0.1:" + this.server.getAddress().getPort());
	}

	void reset() {
		this.requests.clear();
		respond(200, "authenticated".getBytes(StandardCharsets.UTF_8), Map.of("Content-Type", "text/plain"));
	}

	void respond(int status, byte[] body, Map<String, String> headers) {
		this.reply = new Reply(status, body.clone(), Map.copyOf(headers));
	}

	List<CapturedRequest> requests() {
		return List.copyOf(this.requests);
	}

	private void handle(HttpExchange exchange) throws IOException {
		try (exchange) {
			var headers = new HttpHeaders();
			exchange.getRequestHeaders().forEach((name, values) -> headers.put(name, List.copyOf(values)));
			this.requests.add(new CapturedRequest(exchange.getRequestMethod(), exchange.getRequestURI().getRawPath(),
					exchange.getRequestURI().getRawQuery(), headers, exchange.getRequestBody().readAllBytes()));
			Reply current = this.reply;
			current.headers().forEach((name, value) -> exchange.getResponseHeaders().set(name, value));
			boolean noBody = current.body().length == 0 || current.status() == 204 || current.status() == 304;
			exchange.sendResponseHeaders(current.status(), noBody ? -1 : current.body().length);
			if (!noBody) {
				exchange.getResponseBody().write(current.body());
			}
		}
	}

	@Override
	public void close() {
		this.server.stop(0);
	}

	record CapturedRequest(String method, String path, String query, HttpHeaders headers, byte[] body) {

		@Override
		public String toString() {
			return "CapturedRequest[method=" + this.method + ", path=" + this.path + "]";
		}

	}

	private record Reply(int status, byte[] body, Map<String, String> headers) {
	}

}
