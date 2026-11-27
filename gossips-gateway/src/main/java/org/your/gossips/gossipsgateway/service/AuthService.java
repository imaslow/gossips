package org.your.gossips.gossipsgateway.service;

import org.your.gossips.common.dto.AuthResponse;
import org.your.gossips.common.dto.RefreshTokenRequest;
import org.your.gossips.common.dto.UserCredentials;

/**
 * Проксирование auth-операций в security-сервис.
 * Токены не требуются — регистрация/логин/ротация как раз их выпускают.
 */
public interface AuthService {

    AuthResponse register(UserCredentials userCredentials);

    AuthResponse login(UserCredentials userCredentials);

    AuthResponse refresh(RefreshTokenRequest refreshTokenRequest);

    /** Logout; отзывает refresh-токен. */
    void logout(RefreshTokenRequest refreshTokenRequest);
}
