package cl.duoc.eventomax.bff.routing;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rabbit-admin")
public class RabbitAdminRoutingController {
    private final DomainRoutingClient client;
    public RabbitAdminRoutingController(DomainRoutingClient client) { this.client = client; }

    @GetMapping("/queues/{name}")
    ResponseEntity<byte[]> getQueue(HttpServletRequest request) throws IOException {
        return client.rabbitAdmin(HttpMethod.GET, request);
    }
    @PostMapping({"/queues", "/exchanges", "/bindings"})
    ResponseEntity<byte[]> create(HttpServletRequest request) throws IOException {
        return client.rabbitAdmin(HttpMethod.POST, request);
    }
    @DeleteMapping({"/queues/{name}", "/exchanges/{name}", "/bindings"})
    ResponseEntity<byte[]> delete(HttpServletRequest request) throws IOException {
        return client.rabbitAdmin(HttpMethod.DELETE, request);
    }
}
