package org.your.gossips.common.exception;

/**
 * Единый формат тела ошибки для всех сервисов.
 *
 * @param code    машиночитаемый код ({@link ErrorCode})
 * @param message человекочитаемое описание
 */
public record ErrorResponse(ErrorCode code, String message) {

    public static ErrorResponse of(ErrorCode code, String message) {
        return new ErrorResponse(code, message);
    }
}
