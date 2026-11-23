package org.your.gossips.gossipssecurity.common.exception;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Invalid, revoked or expired refresh token");
    }
}
