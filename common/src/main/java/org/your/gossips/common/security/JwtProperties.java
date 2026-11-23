package org.your.gossips.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "gossips.security.jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("15m") Duration accessExpiration,
        @DefaultValue("7d") Duration refreshExpiration) {
}
