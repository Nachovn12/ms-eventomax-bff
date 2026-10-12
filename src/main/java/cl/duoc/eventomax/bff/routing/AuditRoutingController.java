package cl.duoc.eventomax.bff.routing;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
public class AuditRoutingController {

	private final DomainRoutingClient client;

	public AuditRoutingController(DomainRoutingClient client) {
		this.client = client;
	}

	@GetMapping("/timeline")
	ResponseEntity<byte[]> timeline(HttpServletRequest request) throws IOException {
		return this.client.audit(HttpMethod.GET, request);
	}
}
