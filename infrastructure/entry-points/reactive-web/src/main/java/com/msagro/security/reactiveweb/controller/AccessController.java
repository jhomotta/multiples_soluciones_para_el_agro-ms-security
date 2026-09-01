package com.msagro.security.reactiveweb.controller;

import com.msagro.security.model.admin.AssignRoleRequest;
import com.msagro.security.model.admin.GrantAccessRequest;
import com.msagro.security.model.admin.UserAccessView;
import com.msagro.security.model.auth.AuthenticatedPrincipal;
import com.msagro.security.model.generic.GenericResponse;
import com.msagro.security.model.userapplication.UserApplication;
import com.msagro.security.model.userrole.UserRole;
import com.msagro.security.reactiveweb.support.ClientContextResolver;
import com.msagro.security.usecase.business.AccessManagementUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Administration of who may enter which application, and with which roles.
 *
 * <p>These are the routes that change what somebody can do, so each one writes to
 * {@code security_audit} and bumps the target user's {@code security_stamp}.</p>
 */
@RestController
@RequestMapping("/api/v1/access")
@RequiredArgsConstructor
@Tag(name = "Access management", description = "Application access grants and role assignments")
public class AccessController {

    private final AccessManagementUseCase accessManagementUseCase;
    private final ClientContextResolver clientContextResolver;

    @PostMapping
    @PreAuthorize("hasAuthority('USER_GRANT_ACCESS')")
    @Operation(summary = "Grant a user access to an application")
    public Mono<ResponseEntity<GenericResponse<UserApplication>>> grantAccess(
            @Valid @RequestBody GrantAccessRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal actor,
            ServerWebExchange exchange) {
        return accessManagementUseCase.grantAccess(request, actor, clientContextResolver.resolve(exchange))
                .map(grant -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(GenericResponse.success(grant, "Access granted", 201)));
    }

    @DeleteMapping("/{userApplicationId}")
    @PreAuthorize("hasAuthority('USER_GRANT_ACCESS')")
    @Operation(summary = "Revoke an access grant; the row stays for audit purposes")
    public Mono<ResponseEntity<GenericResponse<Object>>> revokeAccess(
            @PathVariable Long userApplicationId,
            @AuthenticationPrincipal AuthenticatedPrincipal actor,
            ServerWebExchange exchange) {
        return accessManagementUseCase
                .revokeAccess(userApplicationId, actor, clientContextResolver.resolve(exchange))
                .then(Mono.fromSupplier(() -> ResponseEntity.ok(
                        GenericResponse.success(null, "Access revoked", 200))));
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_ASSIGN')")
    @Operation(summary = "Assign a role of the same application to an access grant")
    public Mono<ResponseEntity<GenericResponse<UserRole>>> assignRole(
            @Valid @RequestBody AssignRoleRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal actor,
            ServerWebExchange exchange) {
        return accessManagementUseCase.assignRole(request, actor, clientContextResolver.resolve(exchange))
                .map(assignment -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(GenericResponse.success(assignment, "Role assigned", 201)));
    }

    @DeleteMapping("/{userApplicationId}/roles/{roleId}")
    @PreAuthorize("hasAuthority('ROLE_ASSIGN')")
    @Operation(summary = "Revoke a role from an access grant")
    public Mono<ResponseEntity<GenericResponse<Object>>> revokeRole(
            @PathVariable Long userApplicationId,
            @PathVariable Long roleId,
            @AuthenticationPrincipal AuthenticatedPrincipal actor,
            ServerWebExchange exchange) {
        return accessManagementUseCase
                .revokeRole(userApplicationId, roleId, actor, clientContextResolver.resolve(exchange))
                .then(Mono.fromSupplier(() -> ResponseEntity.ok(
                        GenericResponse.success(null, "Role revoked", 200))));
    }

    @GetMapping("/{userApplicationId}")
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "Describe one access grant with its roles and effective permissions")
    public Mono<ResponseEntity<GenericResponse<UserAccessView>>> describeAccess(
            @PathVariable Long userApplicationId) {
        return accessManagementUseCase.describeAccess(userApplicationId)
                .map(view -> ResponseEntity.ok(GenericResponse.success(view, "OK", 200)));
    }

    @GetMapping("/users/{securityUserId}")
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "List every access grant of one user")
    public Mono<ResponseEntity<GenericResponse<List<UserAccessView>>>> listAccessOfUser(
            @PathVariable Long securityUserId) {
        return accessManagementUseCase.listAccessOfUser(securityUserId).collectList()
                .map(views -> ResponseEntity.ok(GenericResponse.success(views, "OK", 200)));
    }
}
