package org.your.gossips.gossipssecurity.service;

import org.your.gossips.common.dto.AuthResponse;
import org.your.gossips.common.dto.UserCredentials;

/**
 * Бизнес-логика регистрации и входа: проверка уникальности, хеширование паролей,
 * выпуск пар токенов.
 */
public interface AuthService {

    /**
     * Регистрирует нового пользователя и сразу выпускает пару токенов.
     *
     * @param userCredentials логин и сырой пароль
     * @return пара access/refresh токенов
     * @throws org.your.gossips.gossipssecurity.common.exception.UserAlreadyExistsException если логин занят
     */
    AuthResponse register(UserCredentials userCredentials);

    /**
     * Аутентифицирует пользователя по логину и паролю.
     *
     * @param userCredentials логин и сырой пароль
     * @return пара access/refresh токенов
     * @throws org.springframework.security.authentication.BadCredentialsException если пара логин/пароль неверна
     */
    AuthResponse login(UserCredentials userCredentials);
}
