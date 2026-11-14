package cl.duoc.eventomax.bff.routing;

import java.io.IOException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rabbit-admin")
@Tag(name = "RabbitMQ Admin", description = "Administración programática de colas, exchanges y bindings vía AmqpAdmin (rol Admin)")
public class RabbitAdminRoutingController {
    private final DomainRoutingClient client;

    public RabbitAdminRoutingController(DomainRoutingClient client) {
        this.client = client;
    }

    @GetMapping("/queues/{name}")
    @Operation(summary = "Consultar información y mensajes de cola")
    @Parameter(name = "name", in = ParameterIn.PATH, required = true, description = "Nombre de la cola RabbitMQ (ej. demo.test)")
    ResponseEntity<byte[]> getQueue(HttpServletRequest request) throws IOException {
        return client.rabbitAdmin(HttpMethod.GET, request);
    }

    @PostMapping("/queues")
    @Operation(summary = "Declarar cola durable en RabbitMQ")
    ResponseEntity<byte[]> createQueue(HttpServletRequest request) throws IOException {
        return client.rabbitAdmin(HttpMethod.POST, request);
    }

    @DeleteMapping("/queues/{name}")
    @Operation(summary = "Eliminar cola en RabbitMQ")
    @Parameter(name = "name", in = ParameterIn.PATH, required = true, description = "Nombre de la cola RabbitMQ")
    ResponseEntity<byte[]> deleteQueue(HttpServletRequest request) throws IOException {
        return client.rabbitAdmin(HttpMethod.DELETE, request);
    }

    @PostMapping("/exchanges")
    @Operation(summary = "Declarar exchange (direct / topic / fanout)")
    ResponseEntity<byte[]> createExchange(HttpServletRequest request) throws IOException {
        return client.rabbitAdmin(HttpMethod.POST, request);
    }

    @DeleteMapping("/exchanges/{name}")
    @Operation(summary = "Eliminar exchange en RabbitMQ")
    @Parameter(name = "name", in = ParameterIn.PATH, required = true, description = "Nombre del exchange RabbitMQ")
    ResponseEntity<byte[]> deleteExchange(HttpServletRequest request) throws IOException {
        return client.rabbitAdmin(HttpMethod.DELETE, request);
    }

    @PostMapping("/bindings")
    @Operation(summary = "Declarar binding exchange → cola")
    ResponseEntity<byte[]> createBinding(HttpServletRequest request) throws IOException {
        return client.rabbitAdmin(HttpMethod.POST, request);
    }

    @DeleteMapping("/bindings")
    @Operation(summary = "Eliminar binding en RabbitMQ")
    ResponseEntity<byte[]> deleteBinding(HttpServletRequest request) throws IOException {
        return client.rabbitAdmin(HttpMethod.DELETE, request);
    }
}
