package cl.duoc.eventomax.bff.routing;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
	ResponseEntity<byte[]> getByProduction(
			@PathVariable("productionId") Long productionId,
			Authentication authentication,
			HttpServletRequest request) throws IOException {

		boolean isStaff = authentication != null && authentication.getAuthorities().stream()
				.anyMatch(a -> "ROLE_Admin".equals(a.getAuthority()) || "ROLE_Productor".equals(a.getAuthority()));

		if (!isStaff) {
			ResponseEntity<byte[]> ownershipCheck = this.client.checkProductionOwnership(productionId, request);
			if (!ownershipCheck.getStatusCode().is2xxSuccessful()) {
				return ownershipCheck;
			}
		}

		return this.client.catalog(HttpMethod.GET, request);
	}

}
