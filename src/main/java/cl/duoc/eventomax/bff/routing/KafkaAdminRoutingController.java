package cl.duoc.eventomax.bff.routing;

import java.io.IOException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kafka-admin")
@Tag(name = "Kafka Admin", description = "Administración programática de tópicos y consumer groups Kafka (rol Admin)")
public class KafkaAdminRoutingController {

    private final DomainRoutingClient client;

    public KafkaAdminRoutingController(DomainRoutingClient client) {
        this.client = client;
    }

    @GetMapping({"/topics", "/topics/{name}", "/consumer-groups", "/consumer-groups/{groupId}"})
    @Operation(summary = "Consultar tópicos o consumer groups en Kafka")
    ResponseEntity<byte[]> getResources(HttpServletRequest request) throws IOException {
        return client.kafkaAdmin(HttpMethod.GET, request);
    }

    @PostMapping("/topics")
    @Operation(summary = "Crear tópico en Kafka")
    ResponseEntity<byte[]> createTopic(HttpServletRequest request) throws IOException {
        return client.kafkaAdmin(HttpMethod.POST, request);
    }

    @PutMapping({"/topics/{name}/configs", "/topics/{name}/partitions"})
    @Operation(summary = "Actualizar configuración o particiones de tópico Kafka")
    ResponseEntity<byte[]> updateTopic(HttpServletRequest request) throws IOException {
        return client.kafkaAdmin(HttpMethod.PUT, request);
    }

    @DeleteMapping({"/topics/{name}", "/consumer-groups/{groupId}"})
    @Operation(summary = "Eliminar tópico o consumer group en Kafka")
    ResponseEntity<byte[]> deleteResource(HttpServletRequest request) throws IOException {
        return client.kafkaAdmin(HttpMethod.DELETE, request);
    }
}
