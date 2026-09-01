package com.msagro.security.model.error;

/** Thrown when a unique constraint would be violated (duplicate username, email, code). Maps to HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
