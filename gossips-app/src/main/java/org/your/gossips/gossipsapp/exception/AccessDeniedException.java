package org.your.gossips.gossipsapp.exception;

/**
 * Доступ к чужому ресурсу (не участник диалога и т.п.).
 * Маппится в 403 FORBIDDEN GlobalExceptionHandler'ом.
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
