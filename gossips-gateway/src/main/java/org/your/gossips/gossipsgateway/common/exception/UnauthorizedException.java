package org.your.gossips.gossipsgateway.common.exception;

/**
 * Нет или невалидный access-токен на защищённом эндпоинте gateway.
 * Маппится в 401 UNAUTHORIZED GlobalExceptionHandler'ом.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException() {
        super("Missing or invalid access token");
    }
}
