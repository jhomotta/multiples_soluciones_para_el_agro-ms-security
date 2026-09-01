package com.msagro.security.model.error;

/** Thrown when a request is well formed but breaks a business rule. Maps to HTTP 400. */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
