package com.msagro.security.usecase;

import com.msagro.security.model.audit.SecurityAudit;
import com.msagro.security.usecase.business.AuditQueryUseCase;
import com.msagro.security.usecase.gateway.securitydb.SecurityAuditRepositoryPort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Read-only access to the security trail. There is no method here that writes, updates or
 * deletes: {@code security_audit} is append-only, and the only component allowed to append is
 * {@link AuditRecorder}.
 */
@Service
public class AuditQueryUseCaseImpl implements AuditQueryUseCase {

    /** Ceiling applied to every query so a caller cannot ask for the whole table. */
    private static final int MAX_LIMIT = 500;
    private static final int DEFAULT_LIMIT = 50;

    private final SecurityAuditRepositoryPort auditRepository;

    public AuditQueryUseCaseImpl(SecurityAuditRepositoryPort auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Override
    public Flux<SecurityAudit> byUser(Long securityUserId, int limit) {
        return auditRepository.findByUser(securityUserId, clamp(limit));
    }

    @Override
    public Flux<SecurityAudit> byApplication(Long applicationId, int limit) {
        return auditRepository.findByApplication(applicationId, clamp(limit));
    }

    @Override
    public Flux<SecurityAudit> byCorrelationId(String correlationId) {
        return auditRepository.findByCorrelationId(correlationId);
    }

    private int clamp(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
