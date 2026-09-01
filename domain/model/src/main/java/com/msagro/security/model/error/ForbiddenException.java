package com.msagro.security.model.error;

/** Thrown when the caller is authenticated but not allowed to perform the action. Maps to HTTP 403. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
