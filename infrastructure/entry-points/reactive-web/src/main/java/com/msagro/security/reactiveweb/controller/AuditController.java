package com.msagro.security.reactiveweb.controller;

import com.msagro.security.model.audit.SecurityAudit;
import com.msagro.security.model.generic.GenericResponse;
import com.msagro.security.usecase.business.AuditQueryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Read-only access to the security trail.
 *
 * <p>There is no POST, PUT, PATCH or DELETE here, and that is the point: {@code security_audit}
 * is append-only, and the only writer is the use-case layer itself.</p>
 */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Append-only security event trail (read-only)")
public class AuditController {

    private final AuditQueryUseCase auditQueryUseCase;

    @GetMapping("/users/{securityUserId}")
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    @Operation(summary = "Most recent security events of one user")
    public Mono<ResponseEntity<GenericResponse<List<SecurityAudit>>>> byUser(
            @PathVariable Long securityUserId,
            @RequestParam(defaultValue = "50") int limit) {
        return auditQueryUseCase.byUser(securityUserId, limit).collectList()
                .map(events -> ResponseEntity.ok(GenericResponse.success(events, "OK", 200)));
    }

    @GetMapping("/applications/{applicationId}")
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    @Operation(summary = "Most recent security events of one application")
    public Mono<ResponseEntity<GenericResponse<List<SecurityAudit>>>> byApplication(
            @PathVariable Long applicationId,
            @RequestParam(defaultValue = "50") int limit) {
        return auditQueryUseCase.byApplication(applicationId, limit).collectList()
                .map(events -> ResponseEntity.ok(GenericResponse.success(events, "OK", 200)));
    }

    @GetMapping("/correlation/{correlationId}")
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    @Operation(summary = "Every event that shares one correlation id")
    public Mono<ResponseEntity<GenericResponse<List<SecurityAudit>>>> byCorrelation(
            @PathVariable String correlationId) {
        return auditQueryUseCase.byCorrelationId(correlationId).collectList()
                .map(events -> ResponseEntity.ok(GenericResponse.success(events, "OK", 200)));
    }
}
