package com.msagro.security.reactiveweb.controller;

import com.msagro.security.model.admin.CreateUserRequest;
import com.msagro.security.model.admin.ReasonRequest;
import com.msagro.security.model.admin.TemporaryPasswordRequest;
import com.msagro.security.model.admin.UpdateUserRequest;
import com.msagro.security.model.admin.UserAdminView;
import com.msagro.security.model.auth.AuthenticatedPrincipal;
import com.msagro.security.model.generic.GenericResponse;
import com.msagro.security.model.role.Role;
import com.msagro.security.model.session.SessionView;
import com.msagro.security.reactiveweb.support.ClientContextResolver;
import com.msagro.security.usecase.business.UserAdministrationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Users of the caller's application, for its administrator (HU-82). Every route needs the
 * permission {@code USER_MANAGE} of that application and works only inside it.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USER_MANAGE')")
@Tag(name = "User administration", description = "Users, roles and sessions of the caller's application")
public class UserAdminController {

    private final UserAdministrationUseCase useCase;
    private final ClientContextResolver clientContextResolver;

    @GetMapping("/roles")
    @Operation(summary = "Roles of the caller's application")
    public Mono<ResponseEntity<GenericResponse<List<Role>>>> roles(
            @AuthenticationPrincipal AuthenticatedPrincipal actor) {
        return useCase.roles(actor).collectList()
                .map(roles -> ResponseEntity.ok(GenericResponse.success(roles, "OK", 200)));
    }

    @GetMapping("/users")
    @Operation(summary = "Users of the caller's application, active or not")
    public Mono<ResponseEntity<GenericResponse<List<UserAdminView>>>> users(
            @AuthenticationPrincipal AuthenticatedPrincipal actor) {
        return useCase.list(actor).collectList()
                .map(users -> ResponseEntity.ok(GenericResponse.success(users, "OK", 200)));
    }

    @PostMapping("/users")
    @Operation(summary = "Create a user with a temporary password and one role")
    public Mono<ResponseEntity<GenericResponse<UserAdminView>>> create(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal actor,
            ServerWebExchange exchange) {
        return useCase.create(request, actor, clientContextResolver.resolve(exchange))
                .map(user -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(GenericResponse.success(user, "User created", 201)));
    }

    @PutMapping("/users/{securityUserId}")
    @Operation(summary = "Edit the names, contact and role of a user")
    public Mono<ResponseEntity<GenericResponse<UserAdminView>>> update(
            @PathVariable Long securityUserId,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal actor,
            ServerWebExchange exchange) {
        return useCase.update(securityUserId, request, actor, clientContextResolver.resolve(exchange))
                .map(user -> ResponseEntity.ok(GenericResponse.success(user, "User updated", 200)));
    }

    @PostMapping("/users/{securityUserId}/deactivate")
    @Operation(summary = "Deactivate a user: no login, every session closed, history kept")
    public Mono<ResponseEntity<GenericResponse<Object>>> deactivate(
            @PathVariable Long securityUserId,
            @Valid @RequestBody ReasonRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal actor,
            ServerWebExchange exchange) {
        return useCase.deactivate(securityUserId, request.getReason(), actor, clientContextResolver.resolve(exchange))
                .then(Mono.fromSupplier(() -> ResponseEntity.ok(
                        GenericResponse.success(null, "User deactivated", 200))));
    }

    @PostMapping("/users/{securityUserId}/activate")
    @Operation(summary = "Activate a deactivated user again")
    public Mono<ResponseEntity<GenericResponse<Object>>> activate(
            @PathVariable Long securityUserId,
            @AuthenticationPrincipal AuthenticatedPrincipal actor,
            ServerWebExchange exchange) {
        return useCase.activate(securityUserId, actor, clientContextResolver.resolve(exchange))
                .then(Mono.fromSupplier(() -> ResponseEntity.ok(
                        GenericResponse.success(null, "User activated", 200))));
    }

    @PostMapping("/users/{securityUserId}/reset-password")
    @Operation(summary = "Give a temporary password; the user changes it at the next login")
    public Mono<ResponseEntity<GenericResponse<Object>>> resetPassword(
            @PathVariable Long securityUserId,
            @Valid @RequestBody TemporaryPasswordRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal actor,
            ServerWebExchange exchange) {
        return useCase.resetPassword(securityUserId, request.getTemporaryPassword(), actor,
                        clientContextResolver.resolve(exchange))
                .then(Mono.fromSupplier(() -> ResponseEntity.ok(GenericResponse.success(
                        null, "Temporary password set. Every session was closed.", 200))));
    }

    @GetMapping("/users/{securityUserId}/sessions")
    @Operation(summary = "Open sessions of a user in this application")
    public Mono<ResponseEntity<GenericResponse<List<SessionView>>>> sessions(
            @PathVariable Long securityUserId,
            @AuthenticationPrincipal AuthenticatedPrincipal actor) {
        return useCase.sessions(securityUserId, actor).collectList()
                .map(sessions -> ResponseEntity.ok(GenericResponse.success(sessions, "OK", 200)));
    }

    @PostMapping("/users/{securityUserId}/sessions/{sessionId}/revoke")
    @Operation(summary = "Close one session of a user, saying why")
    public Mono<ResponseEntity<GenericResponse<Object>>> revokeSession(
            @PathVariable Long securityUserId,
            @PathVariable Long sessionId,
            @Valid @RequestBody ReasonRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal actor,
            ServerWebExchange exchange) {
        return useCase.revokeSession(securityUserId, sessionId, request.getReason(), actor,
                        clientContextResolver.resolve(exchange))
                .then(Mono.fromSupplier(() -> ResponseEntity.ok(
                        GenericResponse.success(null, "Session closed", 200))));
    }
}
