package org.your.gossips.gossipsapp.adapter.out.model;

import java.time.OffsetDateTime;

/**
 * Опубликованный публичный ключ E2E пользователя.
 * Зеркалит таблицу user_keys — jOOQ маппит записи по именам полей.
 */
public record StoredKey(
        String username,
        String publicKey,
        String fingerprint,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
