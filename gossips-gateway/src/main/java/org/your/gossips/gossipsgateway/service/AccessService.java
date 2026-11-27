package org.your.gossips.gossipsgateway.service;

/**
 * Единая точка JWT-гейта gateway: проверка токена и извлечение владельца.
 * Все защищённые эндпоинты проходят через него, чтобы логика «кто ты» жила в одном месте.
 */
public interface AccessService {

    /**
     * Проверяет Bearer-токен заголовка Authorization.
     *
     * @param authorization значение заголовка Authorization (с префиксом «Bearer » или без)
     * @return username владельца токена
     * @throws org.your.gossips.gossipsgateway.common.exception.UnauthorizedException если токен отсутствует или недействителен
     */
    String requireUsername(String authorization);
}
