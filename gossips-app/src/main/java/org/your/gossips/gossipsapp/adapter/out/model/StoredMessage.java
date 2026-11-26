package org.your.gossips.gossipsapp.adapter.out.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Сообщение чата, прочитанное из хранилища.
 * Зеркалит таблицу messages — jOOQ маппит записи по именам полей.
 *
 * @param readAt    время прочтения собеседником; {@code null} — ещё не прочитано
 * @param encrypted {@code true} — content это E2E-ciphertext (base64), сервер не видит plaintext
 */
public record StoredMessage(
        UUID id,
        String username,
        String content,
        OffsetDateTime createdAt,
        UUID conversationId,
        OffsetDateTime readAt,
        boolean encrypted) {
}
