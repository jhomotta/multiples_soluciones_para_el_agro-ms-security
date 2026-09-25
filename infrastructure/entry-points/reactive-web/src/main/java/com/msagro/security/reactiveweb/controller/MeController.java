package com.msagro.security.reactiveweb.controller;

import com.msagro.security.model.admin.ReasonRequest;
import com.msagro.security.model.admin.UserAccessView;
import com.msagro.security.model.auth.AuthenticatedPrincipal;
import com.msagro.security.model.auth.ChangePasswordRequest;
import com.msagro.security.model.generic.GenericResponse;
import com.msagro.security.model.session.SessionView;
import com.msagro.security.reactiveweb.support.ClientContextResolver;
import com.msagro.security.usecase.business.AccessManagementUseCase;
import com.msagro.security.usecase.business.ChangePasswordUseCase;
import com.msagro.security.usecase.business.SessionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/** Endpoints about the caller itself. Every route requires a valid access token. */
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@Tag(name = "Me", description = "The caller's own identity, access and password")
public class MeController {

    private final ChangePasswordUseCase changePasswordUseCase;
    private final AccessManagementUseCase accessManagementUseCase;
    private final SessionUseCase sessionUseCase;
    private final ClientContextResolver clientContextResolver;

    @GetMapping
    @Operation(summary = "Show the identity carried by the current access token",
            description = "The token is checked against the database: a deactivated user, a closed "
                    + "grant or a changed security stamp (password reset, role change) answers 401")
    public Mono<ResponseEntity<GenericResponse<AuthenticatedPrincipal>>> me(
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return sessionUseCase.confirmCurrent(principal)
                .map(confirmed -> ResponseEntity.ok(GenericResponse.success(confirmed, "OK", 200)));
    }

    @GetMapping("/sessions")
    @Operation(summary = "List the caller's open sessions in this application (HU-04)")
    public Mono<ResponseEntity<GenericResponse<List<SessionView>>>> mySessions(
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return sessionUseCase.listSessions(principal.getUserApplicationId())
                .collectList()
                .map(sessions -> ResponseEntity.ok(GenericResponse.success(sessions, "OK", 200)));
    }

    @PostMapping("/sessions/{sessionId}/revoke")
    @Operation(summary = "Close one of the caller's sessions, saying why (HU-04)")
    public Mono<ResponseEntity<GenericResponse<Object>>> revokeMySession(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable Long sessionId,
            @Valid @RequestBody ReasonRequest request,
            ServerWebExchange exchange) {
        return sessionUseCase.revokeSession(principal.getUserApplicationId(), sessionId, request.getReason(),
                        principal, clientContextResolver.resolve(exchange))
                .then(Mono.fromSupplier(() -> ResponseEntity.ok(
                        GenericResponse.success(null, "Session closed", 200))));
    }

    @GetMapping("/access")
    @Operation(summary = "List every application the caller can reach, with its roles and permissions")
    public Mono<ResponseEntity<GenericResponse<List<UserAccessView>>>> myAccess(
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return accessManagementUseCase.listAccessOfUser(principal.getSecurityUserId())
                .collectList()
                .map(views -> ResponseEntity.ok(GenericResponse.success(views, "OK", 200)));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change the caller's own password; every session is revoked afterwards")
    public Mono<ResponseEntity<GenericResponse<Object>>> changePassword(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request,
            ServerWebExchange exchange) {
        return changePasswordUseCase
                .changePassword(principal.getSecurityUserId(), request, clientContextResolver.resolve(exchange))
                .then(Mono.fromSupplier(() -> ResponseEntity.ok(GenericResponse.success(
                        null, "Password changed. Every session was revoked.", 200))));
    }
}
