package org.your.gossips.common.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Эмиссия и валидация JWT для всех сервисов.
 * Подпись HS256, секрет общий: security подписывает токены, gateway проверяет локально.
 */
@Component
public class JwtService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey key;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + properties.accessExpiration().toMillis()))
                .signWith(key)
                .compact();
    }

    /**
     * Проверяет подпись и срок действия токена.
     * Принимает заголовок Authorization как есть: с префиксом "Bearer " или без.
     */
    public boolean isTokenValid(String authorization) {
        String token = stripBearer(authorization);
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Извлекает subject (username) из валидного токена.
     * Вызывать только после {@link #isTokenValid(String)} — на невалидном токене бросит исключение.
     */
    public String extractUsername(String authorization) {
        String token = stripBearer(authorization);
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    private String stripBearer(String authorization) {
        return authorization != null && authorization.startsWith(BEARER_PREFIX)
                ? authorization.substring(BEARER_PREFIX.length())
                : authorization;
    }
}
