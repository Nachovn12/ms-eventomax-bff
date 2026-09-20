package cl.duoc.eventomax.bff.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    @Bean
    OpenAPI eventomaxOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EventoMax BFF API")
                        .version("1.0.0")
                        .description("Contrato externo del Backend For Frontend de EventoMax. "
                                + "Las rutas de negocio requieren un access token JWT de Microsoft Entra ID "
                                + "con scope access_as_user y el rol funcional correspondiente."))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
