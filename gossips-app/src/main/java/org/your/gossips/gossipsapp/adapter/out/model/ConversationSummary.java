package org.your.gossips.gossipsapp.adapter.out.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Сводка диалога для списка: сам диалог + второй участник с его отображаемым именем,
 * последнее сообщение и число непрочитанных.
 */
public record ConversationSummary(
        UUID id,
        String peer,
        String displayName,
        OffsetDateTime createdAt,
        String lastMessage,
        OffsetDateTime lastMessageAt,
        int unreadCount) {
}
