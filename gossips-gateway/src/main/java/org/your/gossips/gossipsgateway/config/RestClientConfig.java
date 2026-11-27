package org.your.gossips.gossipsgateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;
import org.your.gossips.common.logging.RequestIdFilter;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableConfigurationProperties(ClientProperties.class)
public class RestClientConfig {

    /**
     * Прокидывает X-Request-Id из MDC в каждый downstream-запрос — цепочка
     * «gateway → security/app» связывается одним идентификатором в логах.
     */
    private static final ClientHttpRequestInterceptor REQUEST_ID_INTERCEPTOR = new ClientHttpRequestInterceptor() {
        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
                throws IOException {
            request.getHeaders().put(RequestIdFilter.HEADER, List.of(RequestIdFilter.current()));
            return execution.execute(request, body);
        }
    };

    @Bean
    public RestClient securityRestClient(ClientProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.security())
                .requestInterceptor(REQUEST_ID_INTERCEPTOR)
                .build();
    }

    @Bean
    public RestClient appRestClient(ClientProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.app())
                .requestInterceptor(REQUEST_ID_INTERCEPTOR)
                .build();
    }
}
