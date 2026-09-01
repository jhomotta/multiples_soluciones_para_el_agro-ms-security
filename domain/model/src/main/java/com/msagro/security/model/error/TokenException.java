package com.msagro.security.model.error;

/** Thrown when a refresh or reset token is invalid, expired, used or revoked. Maps to HTTP 401. */
public class TokenException extends RuntimeException {

    public TokenException(String message) {
        super(message);
    }
}
