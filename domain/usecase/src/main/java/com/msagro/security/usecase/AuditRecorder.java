package com.msagro.security.usecase;

import com.msagro.security.model.audit.SecurityAudit;
import com.msagro.security.model.auth.ClientContext;
import com.msagro.security.model.enums.AuditEventType;
import com.msagro.security.usecase.gateway.securitydb.SecurityAuditRepositoryPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Writes rows into the append-only {@code security_audit} trail.
 *
 * <p>Auditing must never be the reason a legitimate operation fails, so a write error is
 * swallowed here rather than propagated. Conversely nothing in this class ever updates or
 * deletes a row: the trail only grows.</p>
 */
@Component
public class AuditRecorder {

    private final SecurityAuditRepositoryPort auditRepository;

    public AuditRecorder(SecurityAuditRepositoryPort auditRepository) {
        this.auditRepository = auditRepository;
    }

    /** Records one event and completes, whatever happens to the write. */
    public Mono<Void> record(AuditEventType eventType,
                             Long securityUserId,
                             Long applicationId,
                             boolean success,
                             String description,
                             ClientContext context) {
        return record(eventType, securityUserId, applicationId, success, description, null, null, null, context);
    }

    /** Records one event with the resource it touched and an optional JSON metadata document. */
    public Mono<Void> record(AuditEventType eventType,
                             Long securityUserId,
                             Long applicationId,
                             boolean success,
                             String description,
                             String resource,
                             String resourceId,
                             String metadataJson,
                             ClientContext context) {
        ClientContext ctx = context == null ? ClientContext.empty() : context;

        SecurityAudit audit = SecurityAudit.builder()
                .securityUserId(securityUserId)
                .applicationId(applicationId)
                .eventType(eventType.name())
                .resource(resource)
                .resourceId(resourceId)
                .action(eventType.name())
                .success(success)
                .description(description)
                .metadata(metadataJson)
                .ipAddress(ctx.getIpAddress())
                .userAgent(ctx.getUserAgent())
                .correlationId(ctx.getCorrelationId())
                .createdAt(Instant.now())
                .build();

        return auditRepository.save(audit)
                .onErrorResume(error -> Mono.empty())
                .then();
    }
}
