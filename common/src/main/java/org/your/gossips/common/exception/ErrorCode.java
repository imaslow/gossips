package org.your.gossips.common.exception;

/**
 * Канонические коды ошибок API.
 * Клиенты свитчатся по коду, текст message — только для людей.
 */
public enum ErrorCode {

    /** Нет или невалидный access-токен. */
    UNAUTHORIZED,
    /** Токен валиден, но доступ к ресурсу запрещён (не участник диалога и т.п.). */
    FORBIDDEN,
    /** Неверный логин или пароль. */
    INVALID_CREDENTIALS,
    /** Refresh-токен невалиден, отозван или просрочен. */
    INVALID_REFRESH_TOKEN,
    /** Пользователь с таким логином уже существует. */
    USER_ALREADY_EXISTS,
    /** Ресурс не найден (ключ пользователя и т.п.). */
    NOT_FOUND,
    /** Нарушение валидации входных данных (спека). */
    VALIDATION_FAILED,
    /** Соседний сервис недоступен (сеть). */
    SERVICE_UNAVAILABLE,
    /** Соседний сервис ответил 5xx. */
    UPSTREAM_ERROR
}
