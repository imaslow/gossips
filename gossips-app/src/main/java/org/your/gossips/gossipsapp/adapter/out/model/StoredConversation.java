package org.your.gossips.gossipsapp.adapter.out.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Диалог, прочитанный из хранилища.
 * Зеркалит таблицу conversations.
 *
 * @param type    "pair" — диалог 1-на-1, "group" — групповая беседа
 * @param title   название группы (null для pair)
 * @param createdBy автор создания диалога (null для pair-диалогов до миграции V6)
 */
public record StoredConversation(UUID id, OffsetDateTime createdAt, String type, String title, String createdBy) {
}
