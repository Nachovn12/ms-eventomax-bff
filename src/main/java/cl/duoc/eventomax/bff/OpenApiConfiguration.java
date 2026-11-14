package cl.duoc.eventomax.bff;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "EventoMax BFF API",
        version = "1.0",
        description = "API Gateway / Backend for Frontend de EventoMax. La interfaz Swagger es pública para inspección de contratos; todos los endpoints /api/** requieren Bearer JWT de Microsoft Entra ID con scope access_as_user y rol autorizado (Admin, Productor, Organizador o Auditor)."
    ),
    servers = @Server(url = "/", description = "Servidor actual (AWS API Gateway HTTPS / Local)"),
    security = @SecurityRequirement(name = "BearerAuth")
)
@SecurityScheme(
    name = "BearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class OpenApiConfiguration {
}
