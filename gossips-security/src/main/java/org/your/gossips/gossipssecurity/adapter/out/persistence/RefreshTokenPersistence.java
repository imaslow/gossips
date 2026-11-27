package org.your.gossips.gossipssecurity.adapter.out.persistence;

import org.your.gossips.gossipssecurity.adapter.out.model.StoredRefreshToken;

import java.util.Optional;
import java.util.UUID;

/**
 * Порт для доступа к хранилищу refresh-токенов.
 */
public interface RefreshTokenPersistence {

    /**
     * Ищет токен по хешу (вне зависимости от статуса — активность проверяет вызывающий код).
     */
    Optional<StoredRefreshToken> findByHash(String tokenHash);

    StoredRefreshToken insert(StoredRefreshToken token);

    /**
     * Отзывает токен (проставляет {@code revoked_at}).
     */
    void revoke(UUID id);
}
