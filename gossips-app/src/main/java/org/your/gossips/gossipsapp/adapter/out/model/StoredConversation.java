package org.your.gossips.gossipsapp.adapter.out.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Диалог, прочитанный из хранилища.
 * Зеркалит таблицу conversations.
 */
public record StoredConversation(UUID id, OffsetDateTime createdAt) {
}
