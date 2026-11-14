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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog/services")
@Tag(name = "Catálogo - Servicios", description = "Gestión de servicios técnicos del catálogo (Admin/Productor)")
public class CatalogRoutingController {

	private final DomainRoutingClient client;

	public CatalogRoutingController(DomainRoutingClient client) {
		this.client = client;
	}

	@GetMapping
	@Operation(summary = "Listar servicios del catálogo")
	ResponseEntity<byte[]> list(HttpServletRequest request) throws IOException {
		return this.client.catalog(HttpMethod.GET, request);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Obtener servicio por ID")
	@Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "ID del servicio")
	ResponseEntity<byte[]> getById(HttpServletRequest request) throws IOException {
		return this.client.catalog(HttpMethod.GET, request);
	}

	@PostMapping
	@Operation(summary = "Crear servicio en catálogo (rol Admin)")
	ResponseEntity<byte[]> create(HttpServletRequest request) throws IOException {
		return this.client.catalog(HttpMethod.POST, request);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Actualizar servicio e imagen del catálogo (rol Admin)")
	@Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "ID del servicio")
	ResponseEntity<byte[]> update(HttpServletRequest request) throws IOException {
		return this.client.catalog(HttpMethod.PUT, request);
	}

}
