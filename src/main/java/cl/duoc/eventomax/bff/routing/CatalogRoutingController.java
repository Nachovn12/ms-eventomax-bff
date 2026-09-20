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
@RequestMapping("/api/catalog/services")
@Tag(name = "Catalog", description = "Rutas protegidas del BFF hacia ms-eventomax-catalog")
public class CatalogRoutingController {

    private final DomainRoutingClient client;

    public CatalogRoutingController(DomainRoutingClient client) {
        this.client = client;
    }

    @GetMapping
    @Operation(summary = "Listar servicios", description = "Reenvía el listado del catálogo.")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @ApiResponse(responseCode = "401", description = "JWT ausente o inválido")
    @ApiResponse(responseCode = "403", description = "Scope o rol insuficiente")
    ResponseEntity<byte[]> list(HttpServletRequest request) throws IOException {
        return this.client.catalog(HttpMethod.GET, request);
    }

    @PostMapping
    @Operation(summary = "Crear servicio", description = "Reenvía la creación de un servicio al catálogo.")
    @ApiResponse(responseCode = "201", description = "Servicio creado")
    @ApiResponse(responseCode = "400", description = "Request inválido")
    @ApiResponse(responseCode = "401", description = "JWT ausente o inválido")
    @ApiResponse(responseCode = "403", description = "Scope o rol insuficiente")
    ResponseEntity<byte[]> create(HttpServletRequest request) throws IOException {
        return this.client.catalog(HttpMethod.POST, request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar servicio", description = "Reenvía la actualización de un servicio por ID.")
    @ApiResponse(responseCode = "200", description = "Servicio actualizado")
    @ApiResponse(responseCode = "400", description = "Request inválido")
    @ApiResponse(responseCode = "401", description = "JWT ausente o inválido")
    @ApiResponse(responseCode = "403", description = "Scope o rol insuficiente")
    @ApiResponse(responseCode = "404", description = "Servicio no encontrado")
    ResponseEntity<byte[]> update(HttpServletRequest request) throws IOException {
        return this.client.catalog(HttpMethod.PUT, request);
    }
}
