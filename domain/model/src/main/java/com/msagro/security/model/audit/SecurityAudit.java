package com.msagro.security.model.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * One append-only security event. Rows are written and read but never updated or deleted
 * through the API. {@code metadata} carries a JSON document. Table {@code security_audit}.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SecurityAudit {
    private Long id;
    private Long securityUserId;
    private Long applicationId;
    private String eventType;
    private String resource;
    private String resourceId;
    private String action;
    private Boolean success;
    private String description;
    /** Raw JSON document stored in the {@code jsonb} column. */
    private String metadata;
    private String ipAddress;
    private String userAgent;
    private String correlationId;
    private Instant createdAt;
}
