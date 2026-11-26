package org.your.gossips.gossipssecurity.adapter.out.persistence.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Refresh-токен, сохранённый в хранилище.
 * В БД лежит только SHA-256 хеш — сырой токен в объект не попадает.
 */
public record StoredRefreshToken(
        UUID id,
        UUID userId,
        String tokenHash,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt,
        OffsetDateTime revokedAt) {

    /**
     * @return true, если токен не отозван и не просрочен
     */
    public boolean isActive() {
        return revokedAt == null && expiresAt.isAfter(OffsetDateTime.now());
    }
}
