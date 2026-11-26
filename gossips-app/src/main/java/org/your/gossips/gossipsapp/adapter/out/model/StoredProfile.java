package org.your.gossips.gossipsapp.adapter.out.model;

import java.time.OffsetDateTime;

/**
 * Профиль пользователя, прочитанный из хранилища.
 * Зеркалит таблицу profiles — jOOQ маппит записи по именам полей.
 */
public record StoredProfile(
        String username,
        String displayName,
        String bio,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
