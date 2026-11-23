package org.your.gossips.gossipsgateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(ClientProperties.class)
public class RestClientConfig {

    @Bean
    public RestClient securityRestClient(ClientProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.security())
                .build();
    }

    @Bean
    public RestClient appRestClient(ClientProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.app())
                .build();
    }
}
