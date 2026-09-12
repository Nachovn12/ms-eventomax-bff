package cl.duoc.eventomax.bff.routing;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/productions")
public class ProductionsRoutingController {

	private final DomainRoutingClient client;

	public ProductionsRoutingController(DomainRoutingClient client) {
		this.client = client;
	}

	@PostMapping
	ResponseEntity<byte[]> create(HttpServletRequest request) throws IOException {
		return this.client.productions(HttpMethod.POST, request);
	}

	@GetMapping("/{id}")
	ResponseEntity<byte[]> get(HttpServletRequest request) throws IOException {
		return this.client.productions(HttpMethod.GET, request);
	}

	@GetMapping
	ResponseEntity<byte[]> list(HttpServletRequest request) throws IOException {
		return this.client.productions(HttpMethod.GET, request);
	}

	@PutMapping("/{id}/status")
	ResponseEntity<byte[]> updateStatus(HttpServletRequest request) throws IOException {
		return this.client.productions(HttpMethod.PUT, request);
	}

}
