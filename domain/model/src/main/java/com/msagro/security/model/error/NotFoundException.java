package com.msagro.security.model.error;

/** Thrown when a requested entity does not exist. Maps to HTTP 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String entity, Object id) {
        super(entity + " not found with id: " + id);
    }
}
