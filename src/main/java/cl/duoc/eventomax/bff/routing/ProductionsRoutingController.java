package cl.duoc.eventomax.bff.routing;

import java.io.IOException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
@RequestMapping("/api/productions")
@Tag(name = "Productions", description = "Rutas protegidas del BFF hacia ms-eventomax-productions")
public class ProductionsRoutingController {

    private final DomainRoutingClient client;

    public ProductionsRoutingController(DomainRoutingClient client) {
        this.client = client;
    }

    @PostMapping
    @Operation(summary = "Crear producción", description = "Reenvía el request de creación al microservicio de Productions.")
    @ApiResponse(responseCode = "201", description = "Producción creada")
    @ApiResponse(responseCode = "400", description = "Request inválido")
    @ApiResponse(responseCode = "401", description = "JWT ausente o inválido")
    @ApiResponse(responseCode = "403", description = "Scope o rol insuficiente")
    ResponseEntity<byte[]> create(HttpServletRequest request) throws IOException {
        return this.client.productions(HttpMethod.POST, request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producción", description = "Reenvía la consulta de una producción por ID.")
    @ApiResponse(responseCode = "200", description = "Producción encontrada")
    @ApiResponse(responseCode = "401", description = "JWT ausente o inválido")
    @ApiResponse(responseCode = "403", description = "Scope o rol insuficiente")
    @ApiResponse(responseCode = "404", description = "Producción no encontrada")
    ResponseEntity<byte[]> get(HttpServletRequest request) throws IOException {
        return this.client.productions(HttpMethod.GET, request);
    }

    @GetMapping
    @Operation(summary = "Listar producciones", description = "Reenvía el listado y filtros de Productions.")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @ApiResponse(responseCode = "401", description = "JWT ausente o inválido")
    @ApiResponse(responseCode = "403", description = "Scope o rol insuficiente")
    ResponseEntity<byte[]> list(HttpServletRequest request) throws IOException {
        return this.client.productions(HttpMethod.GET, request);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Actualizar estado", description = "Reenvía una transición de estado al microservicio de Productions.")
    @ApiResponse(responseCode = "200", description = "Estado actualizado")
    @ApiResponse(responseCode = "400", description = "Request inválido")
    @ApiResponse(responseCode = "401", description = "JWT ausente o inválido")
    @ApiResponse(responseCode = "403", description = "Scope o rol insuficiente")
    @ApiResponse(responseCode = "404", description = "Producción no encontrada")
    @ApiResponse(responseCode = "422", description = "Transición de estado inválida")
    ResponseEntity<byte[]> updateStatus(HttpServletRequest request) throws IOException {
        return this.client.productions(HttpMethod.PUT, request);
    }
}
