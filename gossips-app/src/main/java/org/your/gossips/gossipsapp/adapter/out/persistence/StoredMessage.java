package org.your.gossips.gossipsapp.adapter.out.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Сообщение чата, прочитанное из хранилища.
 * Зеркалит таблицу messages — jOOQ маппит записи по именам полей.
 */
public record StoredMessage(UUID id, String username, String content, OffsetDateTime createdAt) {
}
