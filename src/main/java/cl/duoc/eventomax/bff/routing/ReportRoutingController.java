package cl.duoc.eventomax.bff.routing;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/report")
public class ReportRoutingController {

	private final DomainRoutingClient client;

	public ReportRoutingController(DomainRoutingClient client) {
		this.client = client;
	}

	@GetMapping("/kpis")
	ResponseEntity<byte[]> kpis(HttpServletRequest request) throws IOException {
		return this.client.report(HttpMethod.GET, request);
	}

	@GetMapping("/top-services")
	ResponseEntity<byte[]> topServices(HttpServletRequest request) throws IOException {
		return this.client.report(HttpMethod.GET, request);
	}
}
