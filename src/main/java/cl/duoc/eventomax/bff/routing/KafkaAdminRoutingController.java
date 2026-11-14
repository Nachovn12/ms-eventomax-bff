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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kafka-admin")
@Tag(name = "Kafka Admin", description = "Administración programática de tópicos y consumer groups Kafka (rol Admin)")
public class KafkaAdminRoutingController {

    private final DomainRoutingClient client;

    public KafkaAdminRoutingController(DomainRoutingClient client) {
        this.client = client;
    }

    @GetMapping("/topics")
    @Operation(summary = "Listar tópicos del clúster Kafka")
    ResponseEntity<byte[]> listTopics(HttpServletRequest request) throws IOException {
        return client.kafkaAdmin(HttpMethod.GET, request);
    }

    @GetMapping("/topics/{name}")
    @Operation(summary = "Inspeccionar tópico y particiones")
    @Parameter(name = "name", in = ParameterIn.PATH, required = true, description = "Nombre del tópico Kafka")
    ResponseEntity<byte[]> getTopic(HttpServletRequest request) throws IOException {
        return client.kafkaAdmin(HttpMethod.GET, request);
    }

    @PostMapping("/topics")
    @Operation(summary = "Crear tópico en Kafka")
    ResponseEntity<byte[]> createTopic(HttpServletRequest request) throws IOException {
        return client.kafkaAdmin(HttpMethod.POST, request);
    }

    @PutMapping("/topics/{name}/configs")
    @Operation(summary = "Actualizar configuración de tópico Kafka")
    @Parameter(name = "name", in = ParameterIn.PATH, required = true, description = "Nombre del tópico Kafka")
    ResponseEntity<byte[]> updateTopicConfigs(HttpServletRequest request) throws IOException {
        return client.kafkaAdmin(HttpMethod.PUT, request);
    }

    @PutMapping("/topics/{name}/partitions")
    @Operation(summary = "Aumentar particiones de tópico Kafka")
    @Parameter(name = "name", in = ParameterIn.PATH, required = true, description = "Nombre del tópico Kafka")
    ResponseEntity<byte[]> updateTopicPartitions(HttpServletRequest request) throws IOException {
        return client.kafkaAdmin(HttpMethod.PUT, request);
    }

    @DeleteMapping("/topics/{name}")
    @Operation(summary = "Eliminar tópico Kafka")
    @Parameter(name = "name", in = ParameterIn.PATH, required = true, description = "Nombre del tópico Kafka")
    ResponseEntity<byte[]> deleteTopic(HttpServletRequest request) throws IOException {
        return client.kafkaAdmin(HttpMethod.DELETE, request);
    }

    @GetMapping("/consumer-groups")
    @Operation(summary = "Listar consumer groups de Kafka")
    ResponseEntity<byte[]> listConsumerGroups(HttpServletRequest request) throws IOException {
        return client.kafkaAdmin(HttpMethod.GET, request);
    }

    @GetMapping("/consumer-groups/{groupId}")
    @Operation(summary = "Consultar estado y lag de consumer group")
    @Parameter(name = "groupId", in = ParameterIn.PATH, required = true, description = "ID del consumer group")
    ResponseEntity<byte[]> getConsumerGroup(HttpServletRequest request) throws IOException {
        return client.kafkaAdmin(HttpMethod.GET, request);
    }

    @DeleteMapping("/consumer-groups/{groupId}")
    @Operation(summary = "Eliminar consumer group de Kafka")
    @Parameter(name = "groupId", in = ParameterIn.PATH, required = true, description = "ID del consumer group")
    ResponseEntity<byte[]> deleteConsumerGroup(HttpServletRequest request) throws IOException {
        return client.kafkaAdmin(HttpMethod.DELETE, request);
    }
}
