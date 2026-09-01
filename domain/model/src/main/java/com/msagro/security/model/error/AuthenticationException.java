package com.msagro.security.model.error;

/**
 * Thrown when authentication fails. The message is intentionally generic so it never
 * reveals whether the username exists, the password was wrong, or the account is locked.
 * Maps to HTTP 401.
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }
}
