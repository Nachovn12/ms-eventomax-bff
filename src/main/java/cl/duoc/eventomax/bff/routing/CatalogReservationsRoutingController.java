package cl.duoc.eventomax.bff.routing;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog/reservations")
public class CatalogReservationsRoutingController {

	private final DomainRoutingClient client;

	public CatalogReservationsRoutingController(DomainRoutingClient client) {
		this.client = client;
	}

	@GetMapping("/production/{productionId}")
	ResponseEntity<byte[]> getByProduction(HttpServletRequest request) throws IOException {
		return this.client.catalog(HttpMethod.GET, request);
	}

}
