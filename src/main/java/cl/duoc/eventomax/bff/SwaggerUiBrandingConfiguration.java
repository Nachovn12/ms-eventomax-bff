package cl.duoc.eventomax.bff;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpServletRequest;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

/**
 * Customizes Swagger UI branding assets and index page HTML with the EventoMax corporate theme.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "springdoc.swagger-ui.enabled", havingValue = "true", matchIfMissing = true)
public class SwaggerUiBrandingConfiguration {

	private static final String THEME_LINKS = """
			<link rel="icon" type="image/svg+xml" href="./eventomax-logo.svg" />
			<link rel="stylesheet" type="text/css" href="./eventomax-theme.css" />
			</head>""";

	@Bean
	SwaggerIndexTransformer swaggerIndexTransformer(
			SwaggerUiConfigProperties swaggerUiConfig,
			SwaggerUiOAuthProperties swaggerUiOAuthProperties,
			SwaggerWelcomeCommon swaggerWelcomeCommon,
			ObjectMapperProvider objectMapperProvider) {
		return new SwaggerIndexPageTransformer(
				swaggerUiConfig, swaggerUiOAuthProperties, swaggerWelcomeCommon, objectMapperProvider) {
			@Override
			public Resource transform(
					HttpServletRequest request,
					Resource resource,
					ResourceTransformerChain transformerChain) throws IOException {
				if ("index.html".equals(resource.getFilename())) {
					String html;
					try (InputStream in = resource.getInputStream()) {
						html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
					}
					String branded = html
							.replace("<title>Swagger UI</title>", "<title>EventoMax BFF - API Docs</title>")
							.replace("</head>", THEME_LINKS);
					return new TransformedResource(resource, branded.getBytes(StandardCharsets.UTF_8));
				}
				return super.transform(request, resource, transformerChain);
			}
		};
	}

	/**
	 * Serves EventoMax Swagger UI branding CSS and SVG logo under the permitted {@code /swagger-ui/**} path.
	 */
	@RestController
	static class SwaggerBrandingAssetsController {

		private final ClassPathResource themeCss = new ClassPathResource("swagger-ui/eventomax-theme.css");
		private final ClassPathResource logoSvg = new ClassPathResource("swagger-ui/eventomax-logo.svg");

		@GetMapping(value = "/swagger-ui/eventomax-theme.css", produces = "text/css;charset=UTF-8")
		ResponseEntity<Resource> themeCss() {
			return ResponseEntity.ok()
					.contentType(MediaType.parseMediaType("text/css;charset=UTF-8"))
					.body(this.themeCss);
		}

		@GetMapping(value = "/swagger-ui/eventomax-logo.svg", produces = "image/svg+xml;charset=UTF-8")
		ResponseEntity<Resource> logoSvg() {
			return ResponseEntity.ok()
					.contentType(MediaType.parseMediaType("image/svg+xml;charset=UTF-8"))
					.body(this.logoSvg);
		}
	}
}
