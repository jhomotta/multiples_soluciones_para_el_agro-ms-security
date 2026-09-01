package com.msagro.security.usecase.business;

import com.msagro.security.model.audit.SecurityAudit;
import reactor.core.publisher.Flux;

/** Use case: read the append-only security trail. There is no write path here on purpose. */
public interface AuditQueryUseCase {

    Flux<SecurityAudit> byUser(Long securityUserId, int limit);

    Flux<SecurityAudit> byApplication(Long applicationId, int limit);

    Flux<SecurityAudit> byCorrelationId(String correlationId);
}
