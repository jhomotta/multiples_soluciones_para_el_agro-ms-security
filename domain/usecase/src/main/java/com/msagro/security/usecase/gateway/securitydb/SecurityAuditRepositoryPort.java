package com.msagro.security.usecase.gateway.securitydb;

import com.msagro.security.model.audit.SecurityAudit;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Output port for {@code security_audit}. Deliberately offers no update and no delete:
 * the trail is append-only.
 */
public interface SecurityAuditRepositoryPort {

    Mono<SecurityAudit> save(SecurityAudit audit);

    Flux<SecurityAudit> findByUser(Long securityUserId, int limit);

    Flux<SecurityAudit> findByApplication(Long applicationId, int limit);

    Flux<SecurityAudit> findByCorrelationId(String correlationId);
}
