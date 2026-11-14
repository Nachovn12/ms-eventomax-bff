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
@RequestMapping("/api/catalog/equipment")
@Tag(name = "Catálogo - Equipos", description = "Gestión de equipos técnicos e inventario (Admin/Productor)")
public class CatalogEquipmentRoutingController {

	private final DomainRoutingClient client;

	public CatalogEquipmentRoutingController(DomainRoutingClient client) {
		this.client = client;
	}

	@GetMapping
	@Operation(summary = "Listar equipamiento e inventario")
	ResponseEntity<byte[]> list(HttpServletRequest request) throws IOException {
		return this.client.catalog(HttpMethod.GET, request);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Obtener equipo por ID")
	@Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "ID del equipo")
	ResponseEntity<byte[]> get(HttpServletRequest request) throws IOException {
		return this.client.catalog(HttpMethod.GET, request);
	}

	@PostMapping
	@Operation(summary = "Crear equipo e inventario inicial (rol Admin)")
	ResponseEntity<byte[]> create(HttpServletRequest request) throws IOException {
		return this.client.catalog(HttpMethod.POST, request);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Actualizar equipo, stock e imagen (rol Admin)")
	@Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "ID del equipo")
	ResponseEntity<byte[]> update(HttpServletRequest request) throws IOException {
		return this.client.catalog(HttpMethod.PUT, request);
	}

}
