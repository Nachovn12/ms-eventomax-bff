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

    @GetMapping({"/topics", "/topics/{name}"})
    ResponseEntity<byte[]> getTopics(HttpServletRequest request) throws IOException {
        return client.kafkaAdmin(HttpMethod.GET, request);
    }

    @PostMapping("/topics")
    ResponseEntity<byte[]> createTopic(HttpServletRequest request) throws IOException {
        return client.kafkaAdmin(HttpMethod.POST, request);
    }

    @DeleteMapping("/topics/{name}")
    ResponseEntity<byte[]> deleteTopic(HttpServletRequest request) throws IOException {
        return client.kafkaAdmin(HttpMethod.DELETE, request);
    }
}

