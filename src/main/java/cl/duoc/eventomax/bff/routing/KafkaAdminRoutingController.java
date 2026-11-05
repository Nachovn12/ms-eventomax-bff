package cl.duoc.eventomax.bff.routing;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kafka-admin")
public class KafkaAdminRoutingController {

    private final DomainRoutingClient client;

    public KafkaAdminRoutingController(DomainRoutingClient client) {
        this.client = client;
    }

    @GetMapping({"/topics", "/topics/{name}", "/consumer-groups", "/consumer-groups/{groupId}"})
    ResponseEntity<byte[]> getResources(HttpServletRequest request) throws IOException {
        return client.kafkaAdmin(HttpMethod.GET, request);
    }

    @PostMapping("/topics")
    ResponseEntity<byte[]> createTopic(HttpServletRequest request) throws IOException {
        return client.kafkaAdmin(HttpMethod.POST, request);
    }

    @PutMapping({"/topics/{name}/configs", "/topics/{name}/partitions"})
    ResponseEntity<byte[]> updateTopic(HttpServletRequest request) throws IOException {
        return client.kafkaAdmin(HttpMethod.PUT, request);
    }

    @DeleteMapping({"/topics/{name}", "/consumer-groups/{groupId}"})
    ResponseEntity<byte[]> deleteResource(HttpServletRequest request) throws IOException {
        return client.kafkaAdmin(HttpMethod.DELETE, request);
    }
}
