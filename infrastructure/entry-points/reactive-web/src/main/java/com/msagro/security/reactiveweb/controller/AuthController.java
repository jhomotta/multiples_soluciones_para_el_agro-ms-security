package com.msagro.security.reactiveweb.controller;

import com.msagro.security.model.auth.AuthResponse;
import com.msagro.security.model.auth.LoginRequest;
import com.msagro.security.model.auth.LogoutRequest;
import com.msagro.security.model.auth.PasswordResetConfirmRequest;
import com.msagro.security.model.auth.PasswordResetRequest;
import com.msagro.security.model.auth.PasswordResetTicket;
import com.msagro.security.model.auth.RefreshRequest;
import com.msagro.security.model.auth.RegisterRequest;
import com.msagro.security.model.generic.GenericResponse;
import com.msagro.security.reactiveweb.support.ClientContextResolver;
import com.msagro.security.usecase.business.AuthenticateUseCase;
import com.msagro.security.usecase.business.LogoutUseCase;
import com.msagro.security.usecase.business.PasswordResetUseCase;
import com.msagro.security.usecase.business.RefreshTokenUseCase;
import com.msagro.security.usecase.business.RegisterUserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Public authentication endpoints. Everything under {@code /api/v1/auth} is reachable without a
 * token; every other route needs a valid access token.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, refresh, logout and password reset")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final AuthenticateUseCase authenticateUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final PasswordResetUseCase passwordResetUseCase;
    private final ClientContextResolver clientContextResolver;

    @PostMapping("/register")
    @Operation(summary = "Register a person, its credentials and its access to one application")
    public Mono<ResponseEntity<GenericResponse<AuthResponse>>> register(
            @Valid @RequestBody RegisterRequest request, ServerWebExchange exchange) {
        return registerUserUseCase.register(request, clientContextResolver.resolve(exchange))
                .map(auth -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(GenericResponse.success(auth, "User registered", 201)));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate against one application and receive a token pair")
    public Mono<ResponseEntity<GenericResponse<AuthResponse>>> login(
            @Valid @RequestBody LoginRequest request, ServerWebExchange exchange) {
        return authenticateUseCase.login(request, clientContextResolver.resolve(exchange))
                .map(auth -> ResponseEntity.ok(GenericResponse.success(auth, "Authenticated", 200)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate a refresh token into a new token pair")
    public Mono<ResponseEntity<GenericResponse<AuthResponse>>> refresh(
            @Valid @RequestBody RefreshRequest request, ServerWebExchange exchange) {
        return refreshTokenUseCase.refresh(request.getRefreshToken(), clientContextResolver.resolve(exchange))
                .map(auth -> ResponseEntity.ok(GenericResponse.success(auth, "Token refreshed", 200)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the presented refresh token, or every session of the access grant")
    public Mono<ResponseEntity<GenericResponse<Object>>> logout(
            @Valid @RequestBody LogoutRequest request, ServerWebExchange exchange) {
        return logoutUseCase.logout(request, clientContextResolver.resolve(exchange))
                .then(Mono.fromSupplier(() -> ResponseEntity.ok(
                        GenericResponse.success(null, "Logged out", 200))));
    }

    @PostMapping("/password-reset/request")
    @Operation(summary = "Start a password reset; the answer never reveals whether the user exists")
    public Mono<ResponseEntity<GenericResponse<PasswordResetTicket>>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request, ServerWebExchange exchange) {
        return passwordResetUseCase.request(request, clientContextResolver.resolve(exchange))
                .map(ticket -> ResponseEntity.ok(
                        GenericResponse.success(ticket, ticket.getMessage(), 200)));
    }

    @PostMapping("/password-reset/confirm")
    @Operation(summary = "Finish a password reset with the single-use token")
    public Mono<ResponseEntity<GenericResponse<Object>>> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request, ServerWebExchange exchange) {
        return passwordResetUseCase.confirm(request, clientContextResolver.resolve(exchange))
                .then(Mono.fromSupplier(() -> ResponseEntity.ok(GenericResponse.success(
                        null, "Password updated. Every session was revoked.", 200))));
    }
}
