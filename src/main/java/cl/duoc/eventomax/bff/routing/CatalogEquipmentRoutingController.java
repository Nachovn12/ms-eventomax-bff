package cl.duoc.eventomax.bff.routing;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog/equipment")
public class CatalogEquipmentRoutingController {

	private final DomainRoutingClient client;

	public CatalogEquipmentRoutingController(DomainRoutingClient client) {
		this.client = client;
	}

	@GetMapping
	ResponseEntity<byte[]> list(HttpServletRequest request) throws IOException {
		return this.client.catalog(HttpMethod.GET, request);
	}

	@GetMapping("/{id}")
	ResponseEntity<byte[]> get(HttpServletRequest request) throws IOException {
		return this.client.catalog(HttpMethod.GET, request);
	}

	@PostMapping
	ResponseEntity<byte[]> create(HttpServletRequest request) throws IOException {
		return this.client.catalog(HttpMethod.POST, request);
	}

}
