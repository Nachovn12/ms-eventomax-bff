package cl.duoc.eventomax.bff.routing;

import java.io.IOException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rabbit-admin")
@Tag(name = "RabbitMQ Admin", description = "Administración programática de colas, exchanges y bindings vía AmqpAdmin (rol Admin)")
public class RabbitAdminRoutingController {
    private final DomainRoutingClient client;
    public RabbitAdminRoutingController(DomainRoutingClient client) { this.client = client; }

    @GetMapping("/queues/{name}")
    @Operation(summary = "Consultar información de cola en RabbitMQ")
    ResponseEntity<byte[]> getQueue(HttpServletRequest request) throws IOException {
        return client.rabbitAdmin(HttpMethod.GET, request);
    }
    @PostMapping({"/queues", "/exchanges", "/bindings"})
    @Operation(summary = "Declarar cola, exchange o binding en RabbitMQ")
    ResponseEntity<byte[]> create(HttpServletRequest request) throws IOException {
        return client.rabbitAdmin(HttpMethod.POST, request);
    }
    @DeleteMapping({"/queues/{name}", "/exchanges/{name}", "/bindings"})
    @Operation(summary = "Eliminar cola, exchange o binding en RabbitMQ")
    ResponseEntity<byte[]> delete(HttpServletRequest request) throws IOException {
        return client.rabbitAdmin(HttpMethod.DELETE, request);
    }
}
