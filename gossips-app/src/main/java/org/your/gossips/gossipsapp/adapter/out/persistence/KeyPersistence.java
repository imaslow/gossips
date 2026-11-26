package org.your.gossips.gossipsapp.adapter.out.persistence;

import org.your.gossips.gossipsapp.adapter.out.model.StoredKey;
import org.your.gossips.gossipsapp.adapter.out.model.StoredKeyVerification;

import java.util.Optional;

/**
 * Порт хранения публичных ключей E2E и их верификаций.
 * Приватные ключи через этот порт не проходят — только опубликованные клиентом.
 */
public interface KeyPersistence {

    /**
     * Публикует (создаёт или заменяет) публичный ключ пользователя.
     */
    StoredKey upsert(StoredKey key);

    /**
     * Опубликованный ключ пользователя, если есть.
     */
    Optional<StoredKey> findByUsername(String username);

    /**
     * Запись о верификации: кем, чей ключ, какой отпечаток.
     */
    Optional<StoredKeyVerification> findVerification(String verifierUsername, String verifiedUsername);

    /**
     * Сохраняет (создаёт или заменяет) верификацию ключа.
     */
    StoredKeyVerification upsertVerification(String verifierUsername, String verifiedUsername, String fingerprint);

    /**
     * Удаляет верификацию. Идемпотентно — отсутствующая верификация не ошибка.
     */
    void deleteVerification(String verifierUsername, String verifiedUsername);
}
