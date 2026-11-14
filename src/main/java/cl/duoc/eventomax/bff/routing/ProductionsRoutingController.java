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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/productions")
@Tag(name = "Producciones", description = "Gestión del ciclo de vida de producciones, tickets de cuadrilla y actas PDF")
public class ProductionsRoutingController {

	private final DomainRoutingClient client;

	public ProductionsRoutingController(DomainRoutingClient client) {
		this.client = client;
	}

	@PostMapping
	@Operation(summary = "Crear producción", description = "Crea una nueva producción con servicios contratados (roles Productor u Organizador).")
	ResponseEntity<byte[]> create(HttpServletRequest request) throws IOException {
		return this.client.productions(HttpMethod.POST, request);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Obtener producción por ID")
	@Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "ID de la producción")
	ResponseEntity<byte[]> get(HttpServletRequest request) throws IOException {
		return this.client.productions(HttpMethod.GET, request);
	}

	@GetMapping("/{id}/services")
	@Operation(summary = "Listar servicios contratados de una producción")
	@Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "ID de la producción")
	ResponseEntity<byte[]> getServices(HttpServletRequest request) throws IOException {
		return this.client.productions(HttpMethod.GET, request);
	}

	@GetMapping
	@Operation(summary = "Listar producciones", description = "Retorna producciones paginadas o filtradas según rol.")
	ResponseEntity<byte[]> list(HttpServletRequest request) throws IOException {
		return this.client.productions(HttpMethod.GET, request);
	}

	@PutMapping("/{id}/status")
	@Operation(summary = "Actualizar estado operativo de producción", description = "Transiciona estado (SOLICITADO, CONFIRMADO, EN_MONTAJE, EN_EJECUCION, CERRADO) disparando eventos Kafka/RabbitMQ.")
	@Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "ID de la producción")
	ResponseEntity<byte[]> updateStatus(HttpServletRequest request) throws IOException {
		return this.client.productions(HttpMethod.PUT, request);
	}

	@PostMapping("/{id}/quote-command/republish")
	@Operation(summary = "Republicar comando de acta PDF en RabbitMQ")
	@Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "ID de la producción")
	ResponseEntity<byte[]> republishQuoteCommand(HttpServletRequest request) throws IOException {
		return this.client.productions(HttpMethod.POST, request);
	}

	@GetMapping("/{id}/crew-ticket")
	@Operation(summary = "Obtener ticket de cuadrilla técnica")
	ResponseEntity<byte[]> getCrewTicket(@PathVariable("id") Long id, HttpServletRequest request) {
		var check = this.client.checkProductionOwnership(id, request);
		if (!check.getStatusCode().is2xxSuccessful()) {
			return check;
		}
		return this.client.notifyTicket(id, request);
	}

	@GetMapping("/{id}/quote-pdf")
	@Operation(summary = "Descargar Acta de Cierre / Cotización PDF")
	ResponseEntity<byte[]> getQuotePdf(@PathVariable("id") Long id, HttpServletRequest request) {
		var check = this.client.checkProductionOwnership(id, request);
		if (!check.getStatusCode().is2xxSuccessful()) {
			return check;
		}
		return this.client.notifyQuotePdf(id, request);
	}

}
