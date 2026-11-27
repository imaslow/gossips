package org.your.gossips.gossipsapp.adapter.out.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Сводка диалога для списка: тип, название (для групп), второй участник
 * с его отображаемым именем (только для pair — у групп null),
 * последнее сообщение и число непрочитанных.
 */
public record ConversationSummary(
        UUID id,
        String peer,
        String displayName,
        String type,
        String title,
        OffsetDateTime createdAt,
        String lastMessage,
        OffsetDateTime lastMessageAt,
        boolean lastMessageEncrypted,
        int unreadCount) {
}
