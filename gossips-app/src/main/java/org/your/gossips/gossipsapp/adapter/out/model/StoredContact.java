package org.your.gossips.gossipsapp.adapter.out.model;

import java.time.OffsetDateTime;

/**
 * Запись таблицы contacts + отображаемое имя контакта (join с profiles,
 * при отсутствии профиля совпадает с логином).
 */
public record StoredContact(String ownerUsername, String contactUsername, String displayName, OffsetDateTime createdAt) {
}
