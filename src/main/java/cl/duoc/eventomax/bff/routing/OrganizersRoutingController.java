package cl.duoc.eventomax.bff.routing;

import java.io.IOException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Organizadores", description = "Gestión de contacto corporativo de organizadores")
public class OrganizersRoutingController {

	private final DomainRoutingClient client;

	public OrganizersRoutingController(DomainRoutingClient client) {
		this.client = client;
	}

	@GetMapping("/eligible")
	@Operation(summary = "Listar organizadores elegibles", description = "Consulta paginada de organizadores elegibles (Entra ID y clientes externos registrados) para el selector de nueva producción. Restringido a Admin y Productor.")
	@Parameter(name = "q", in = ParameterIn.QUERY, required = false, description = "Filtro de búsqueda por nombre")
	@Parameter(name = "page", in = ParameterIn.QUERY, required = false, description = "Número de página (base 0)")
	@Parameter(name = "size", in = ParameterIn.QUERY, required = false, description = "Tamaño de página (acotado por backend)")
	ResponseEntity<byte[]> listEligible(HttpServletRequest request) throws IOException {
		return this.client.productions(HttpMethod.GET, request);
	}

	@GetMapping("/{organizerId}/contact")
	@Operation(summary = "Obtener contacto corporativo del organizador")
	@Parameter(name = "organizerId", in = ParameterIn.PATH, required = true, description = "Identificador del organizador")
	ResponseEntity<byte[]> getContact(HttpServletRequest request) throws IOException {
		return this.client.productions(HttpMethod.GET, request);
	}

	@PutMapping("/{organizerId}/contact")
	@Operation(summary = "Actualizar contacto corporativo del organizador")
	@Parameter(name = "organizerId", in = ParameterIn.PATH, required = true, description = "Identificador del organizador")
	ResponseEntity<byte[]> updateContact(HttpServletRequest request) throws IOException {
		return this.client.productions(HttpMethod.PUT, request);
	}

}

