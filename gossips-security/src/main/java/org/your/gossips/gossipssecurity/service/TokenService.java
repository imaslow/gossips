package org.your.gossips.gossipssecurity.service;

import org.your.gossips.common.dto.AuthResponse;

import java.util.UUID;

/**
 * Выпуск, ротация и отзыв пар токенов.
 * Access — короткоживущий JWT (stateless), refresh — долгоживущая случайная строка
 * (в БД в виде SHA-256 хеша, отзываемая).
 */
public interface TokenService {

    /**
     * Выпускает новую пару токенов для пользователя (register/login).
     */
    AuthResponse issueTokens(UUID userId, String username);

    /**
     * Обмен refresh-токена на новую пару (ротация: старый отзывается).
     *
     * @throws org.your.gossips.gossipssecurity.common.exception.InvalidRefreshTokenException если токен неактивен
     */
    AuthResponse rotate(String refreshToken);

    /**
     * Отзыв refresh-токена (logout). Access-токен доживает свой срок сам.
     *
     * @throws org.your.gossips.gossipssecurity.common.exception.InvalidRefreshTokenException если токен неактивен
     */
    void revoke(String refreshToken);
}
