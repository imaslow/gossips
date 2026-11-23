package org.your.gossips.gossipsgateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gossips.clients")
public record ClientProperties(String security, String app) {
}
