package cl.duoc.eventomax.bff;

import java.util.List;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
		info = @Info(
				title = "EventoMax BFF API",
				version = "1.0",
				description = """
						Backend for Frontend de **EventoMax** desplegado detrás de AWS API Gateway y Nginx Edge Proxy.

						La interfaz Swagger UI es pública para inspección de contratos OpenAPI; cada endpoint `/api/**` exige **Bearer JWT** de Microsoft Entra ID con scope `access_as_user` y rol autorizado (`Admin`, `Productor`, `Organizador`, `Auditor`). Para ejecutar pruebas en vivo desde **Try it out**, haz clic en **Authorize** e ingresa el Access Token del Portal Web.

						| Dominio | Roles Permitidos | Servicio Downstream |
						| :--- | :--- | :--- |
						| **Producciones y Actas** | `Admin`, `Productor`, `Organizador` | `ms-eventomax-productions` / `notify` |
						| **Catálogo y Equipos** | `Admin` (escritura), `Productor` (lectura) | `ms-eventomax-catalog` |
						| **Reportes y Auditoría** | `Admin` (`report` y `audit`), `Auditor` (`audit`) | `ms-eventomax-report` / `audit` |
						| **Admin RabbitMQ y Kafka** | `Admin` | `rabbit-admin` / `kafka-admin` |
						"""),
		servers = @Server(url = "/", description = "Servidor actual (AWS API Gateway HTTPS / Local)"),
		security = @SecurityRequirement(name = "BearerAuth"),
		externalDocs = @ExternalDocumentation(
				description = "Abrir Portal Web EventoMax (Amazon S3)",
				url = "https://eventomax-production-portal-app.s3.us-east-1.amazonaws.com/index.html")
)
@SecurityScheme(
		name = "BearerAuth",
		type = SecuritySchemeType.HTTP,
		scheme = "bearer",
		bearerFormat = "JWT",
		description = "Access Token JWT emitido por Microsoft Entra ID para api://a1a87bcd-fed7-4d65-83c3-338389b80093"
)
public class OpenApiConfiguration {

	@Bean
	OpenApiCustomizer eventomaxTagOrderCustomizer() {
		return openApi -> openApi.setTags(List.of(
				new Tag().name("Producciones").description("Gestión del ciclo de vida de producciones, tickets de cuadrilla y actas PDF"),
				new Tag().name("Organizadores").description("Gestión de contacto corporativo de organizadores"),
				new Tag().name("Catálogo - Servicios").description("Servicios técnicos del catálogo (Admin/Productor)"),
				new Tag().name("Catálogo - Equipos").description("Equipamiento técnico e inventario (Admin/Productor)"),
				new Tag().name("Catálogo - Reservas").description("Consulta de reservas de inventario asociadas a producciones"),
				new Tag().name("Reportes").description("KPIs operativos y ranking de servicios (proyección Kafka)"),
				new Tag().name("Auditoría").description("Historial cronológico de eventos Kafka (Admin/Auditor)"),
				new Tag().name("RabbitMQ Admin").description("Administración programática de colas, exchanges y bindings vía AmqpAdmin (rol Admin)"),
				new Tag().name("Kafka Admin").description("Administración programática de tópicos y consumer groups Kafka (rol Admin)")
		));
	}
}
