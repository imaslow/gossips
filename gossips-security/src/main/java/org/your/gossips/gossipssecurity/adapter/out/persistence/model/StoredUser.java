package org.your.gossips.gossipssecurity.adapter.out.persistence.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Пользователь, сохранённый в хранилище.
 * Пароль хранится только в виде хеша — сырой пароль в объект не попадает.
 */
public record StoredUser(UUID id, String username, String passwordHash, OffsetDateTime createdAt) {
}
