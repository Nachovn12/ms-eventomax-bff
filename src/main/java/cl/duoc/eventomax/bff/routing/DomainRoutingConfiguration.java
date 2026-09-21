package cl.duoc.eventomax.bff.routing;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class DomainRoutingConfiguration {

	@Bean
	HttpClient domainHttpClient(@Value("${eventomax.downstream.connect-timeout:3s}") Duration connectTimeout) {
		return HttpClient.newBuilder()
				.connectTimeout(connectTimeout)
				.followRedirects(HttpClient.Redirect.NEVER)
				.build();
	}

	@Bean
	JdkClientHttpRequestFactory domainRequestFactory(HttpClient domainHttpClient,
			@Value("${eventomax.downstream.read-timeout:10s}") Duration readTimeout) {
		var factory = new JdkClientHttpRequestFactory(domainHttpClient);
		factory.setReadTimeout(readTimeout);
		return factory;
	}

	@Bean
	DomainRoutingClient domainRoutingClient(ObjectProvider<RestClient.Builder> builders,
			JdkClientHttpRequestFactory domainRequestFactory,
			@Value("${eventomax.downstream.productions-base-url}") URI productionsBaseUrl,
			@Value("${eventomax.downstream.catalog-base-url}") URI catalogBaseUrl) {
		// Use Boot's builder when available, without requiring another starter.
		RestClient client = builders.getIfAvailable(RestClient::builder).clone()
				.requestFactory(domainRequestFactory).build();
		return new DomainRoutingClient(client, productionsBaseUrl, catalogBaseUrl);
	}

}
