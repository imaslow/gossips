package org.your.gossips.gossipsgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.your.gossips.common.security.JwtProperties;
import org.your.gossips.common.security.JwtService;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
// common не входит в component-scan gateway — подключаем явно
@Import(JwtService.class)
public class GossipsGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GossipsGatewayApplication.class, args);
    }

}
