package cl.duoc.eventomax.bff.routing;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizers")
public class OrganizersRoutingController {

	private final DomainRoutingClient client;

	public OrganizersRoutingController(DomainRoutingClient client) {
		this.client = client;
	}

	@GetMapping("/{organizerId}/contact")
	ResponseEntity<byte[]> getContact(HttpServletRequest request) throws IOException {
		return this.client.productions(HttpMethod.GET, request);
	}

	@PutMapping("/{organizerId}/contact")
	ResponseEntity<byte[]> updateContact(HttpServletRequest request) throws IOException {
		return this.client.productions(HttpMethod.PUT, request);
	}

}

