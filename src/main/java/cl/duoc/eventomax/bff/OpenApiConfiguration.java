package cl.duoc.eventomax.bff;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
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
		tags = {
				@Tag(name = "Producciones", description = "Gestión del ciclo de vida de producciones, tickets de cuadrilla y actas PDF"),
				@Tag(name = "Organizadores", description = "Gestión de contacto corporativo de organizadores"),
				@Tag(name = "Catálogo - Servicios", description = "Servicios técnicos del catálogo (Admin/Productor)"),
				@Tag(name = "Catálogo - Equipos", description = "Equipamiento técnico e inventario (Admin/Productor)"),
				@Tag(name = "Catálogo - Reservas", description = "Consulta de reservas de inventario asociadas a producciones"),
				@Tag(name = "Reportes", description = "KPIs operativos y ranking de servicios (proyección Kafka)"),
				@Tag(name = "Auditoría", description = "Historial cronológico de eventos Kafka (Admin/Auditor)"),
				@Tag(name = "RabbitMQ Admin", description = "Administración programática de colas, exchanges y bindings vía AmqpAdmin (rol Admin)"),
				@Tag(name = "Kafka Admin", description = "Administración programática de tópicos y consumer groups Kafka (rol Admin)")
		},
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
}
