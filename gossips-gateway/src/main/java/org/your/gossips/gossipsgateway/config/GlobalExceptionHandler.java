package org.your.gossips.gossipsgateway.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.your.gossips.gossipsgateway.common.exception.ServiceUnavailableException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<Void> handleServiceUnavailable(ServiceUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    // HTTP-ошибки downstream (401, 409, ...) пробрасываем клиенту как есть
    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<Void> handleClientError(HttpClientErrorException ex) {
        return ResponseEntity.status(ex.getStatusCode()).build();
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<Void> handleServerError(HttpServerErrorException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
    }
}
