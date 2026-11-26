package org.your.gossips.gossipsapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки пагинации сообщений (application.yml: gossips.pagination).
 */
@ConfigurationProperties(prefix = "gossips.pagination")
public record PaginationProperties(int defaultLimit, int maxLimit) {
}
