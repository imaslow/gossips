package org.your.gossips.gossipsapp.exception;

/**
 * Пользователь ещё не опубликовал свой публичный ключ.
 */
public class KeyNotFoundException extends RuntimeException {

    public KeyNotFoundException(String username) {
        super("User " + username + " has not published a key");
    }
}
