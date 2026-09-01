package com.msagro.security.reactiveweb.exception;

import com.msagro.security.model.error.AuthenticationException;
import com.msagro.security.model.error.BusinessException;
import com.msagro.security.model.error.ConflictException;
import com.msagro.security.model.error.ForbiddenException;
import com.msagro.security.model.error.NotFoundException;
import com.msagro.security.model.error.TokenException;
import com.msagro.security.model.generic.GenericResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

/**
 * Turns domain and framework errors into one uniform JSON envelope with the right status code,
 * so controllers never build an error body themselves.
 *
 * <p>Two rules shape what reaches the caller. Authentication and token failures answer with the
 * message the domain chose and nothing more — those messages are deliberately vague, and adding
 * detail here would undo that. And an unexpected exception is logged in full but answered with a
 * fixed sentence, because a raw exception message is a description of the server's internals.</p>
 */
@RestControllerAdvice
@Order(-2)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public Mono<ResponseEntity<GenericResponse<Object>>> handleNotFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public Mono<ResponseEntity<GenericResponse<Object>>> handleConflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public Mono<ResponseEntity<GenericResponse<Object>>> handleAuthentication(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(TokenException.class)
    public Mono<ResponseEntity<GenericResponse<Object>>> handleToken(TokenException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public Mono<ResponseEntity<GenericResponse<Object>>> handleForbidden(ForbiddenException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Mono<ResponseEntity<GenericResponse<Object>>> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "You do not have the permission required for this operation");
    }

    @ExceptionHandler(BusinessException.class)
    public Mono<ResponseEntity<GenericResponse<Object>>> handleBusiness(BusinessException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<GenericResponse<Object>>> handleValidation(WebExchangeBindException ex) {
        String details = ex.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "Validation failed: " + details);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<GenericResponse<Object>>> handleBadRequest(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * A constraint the use case did not pre-check still has to answer as a conflict rather than
     * a 500 — the composite foreign keys on {@code user_role} and {@code role_permission} are the
     * last line of defence, and they are supposed to fire.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Mono<ResponseEntity<GenericResponse<Object>>> handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("Database constraint rejected the operation", ex);
        return build(HttpStatus.CONFLICT,
                "The operation violates a database constraint (duplicate value, or a reference "
                        + "that crosses applications)");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<GenericResponse<Object>>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        return build(status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status,
                ex.getReason() == null ? "Request could not be processed" : ex.getReason());
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<GenericResponse<Object>>> handleGeneric(Exception ex) {
        log.error("Unhandled error while processing the request", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error. Check the server logs.");
    }

    private Mono<ResponseEntity<GenericResponse<Object>>> build(HttpStatus status, String message) {
        GenericResponse<Object> body = GenericResponse.error(message, status.value());
        return Mono.just(ResponseEntity.status(status).body(body));
    }
}
