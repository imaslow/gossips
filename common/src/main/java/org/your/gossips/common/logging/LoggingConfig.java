package org.your.gossips.common.logging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Регистрация сквозного идентификатора запроса. Common не входит в component-scan
 * сервисов, поэтому конфигурацию подключают явно через {@code @Import(LoggingConfig.class)}
 * рядом с {@code @Import(JwtService.class)}.
 */
@Configuration
public class LoggingConfig {

    @Bean
    public RequestIdFilter requestIdFilter() {
        return new RequestIdFilter();
    }
}
