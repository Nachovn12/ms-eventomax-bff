package cl.duoc.eventomax.bff.routing;

import java.io.IOException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
@Tag(name = "Auditoría", description = "Línea de tiempo inmutable de eventos Kafka (Admin/Auditor)")
public class AuditRoutingController {

	private final DomainRoutingClient client;

	public AuditRoutingController(DomainRoutingClient client) {
		this.client = client;
	}

	@GetMapping("/timeline")
	@Operation(summary = "Consultar línea de tiempo de auditoría")
	ResponseEntity<byte[]> timeline(HttpServletRequest request) throws IOException {
		return this.client.audit(HttpMethod.GET, request);
	}

	@GetMapping("/timeline/latest")
	@Operation(summary = "Consultar último estado compactado por producción")
	ResponseEntity<byte[]> latestSnapshots(HttpServletRequest request) throws IOException {
		return this.client.audit(HttpMethod.GET, request);
	}
}
