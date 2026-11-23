package org.your.gossips.gossipssecurity.adapter.out.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Refresh-токен, сохранённый в хранилище.
 * Сам токен в БД не попадает — только его SHA-256 хеш.
 */
public record StoredRefreshToken(
        UUID id,
        UUID userId,
        String tokenHash,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt,
        OffsetDateTime revokedAt) {

    public boolean isActive() {
        return revokedAt == null && expiresAt.isAfter(OffsetDateTime.now());
    }
}
