package org.your.gossips.gossipsapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.your.gossips.common.security.JwtProperties;
import org.your.gossips.common.security.JwtService;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableConfigurationProperties(JwtProperties.class)
@Import(JwtService.class)
// common не входит в component-scan — JwtService подключаем явно (валидация токенов WS)
public class GossipsAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(GossipsAppApplication.class, args);
    }

}
