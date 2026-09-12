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
@RequestMapping("/api/catalog/services")
public class CatalogRoutingController {

	private final DomainRoutingClient client;

	public CatalogRoutingController(DomainRoutingClient client) {
		this.client = client;
	}

	@GetMapping
	ResponseEntity<byte[]> list(HttpServletRequest request) throws IOException {
		return this.client.catalog(HttpMethod.GET, request);
	}

	@PostMapping
	ResponseEntity<byte[]> create(HttpServletRequest request) throws IOException {
		return this.client.catalog(HttpMethod.POST, request);
	}

	@PutMapping("/{id}")
	ResponseEntity<byte[]> update(HttpServletRequest request) throws IOException {
		return this.client.catalog(HttpMethod.PUT, request);
	}

}
