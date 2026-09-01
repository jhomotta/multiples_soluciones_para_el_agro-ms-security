package com.msagro.security.drivenadapters.securitydb.adapter;

import com.msagro.security.drivenadapters.securitydb.mapper.DateTimeMapper;
import com.msagro.security.model.audit.SecurityAudit;
import com.msagro.security.usecase.gateway.securitydb.SecurityAuditRepositoryPort;
import io.r2dbc.spi.Readable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * R2DBC implementation of the security-audit output port.
 *
 * <p>Append-only by construction: this class offers an insert and three reads, and no statement
 * anywhere in it updates or deletes. {@code metadata} is a {@code jsonb} column bound from and
 * rendered back to plain JSON text, and {@code ip_address} is an {@code inet} handled the same
 * way as in {@link RefreshTokenAdapter}.</p>
 */
@Component
public class SecurityAuditAdapter implements SecurityAuditRepositoryPort {

    private static final String SELECT_COLUMNS = """
            SELECT id, security_user_id, application_id, event_type, resource, resource_id, action,
                   success, description, metadata::text AS metadata, host(ip_address) AS ip_address,
                   user_agent, correlation_id, created_at
            """;

    private final DatabaseClient client;
    private final DateTimeMapper dateTimeMapper;

    public SecurityAuditAdapter(DatabaseClient client, DateTimeMapper dateTimeMapper) {
        this.client = client;
        this.dateTimeMapper = dateTimeMapper;
    }

    @Override
    public Mono<SecurityAudit> save(SecurityAudit audit) {
        DatabaseClient.GenericExecuteSpec spec = client.sql("""
                        INSERT INTO security_audit (security_user_id, application_id, event_type, resource,
                                                    resource_id, action, success, description, metadata,
                                                    ip_address, user_agent, correlation_id, created_at)
                        VALUES (:securityUserId, :applicationId, :eventType, :resource, :resourceId,
                                :action, :success, :description, CAST(:metadata AS jsonb),
                                CAST(:ipAddress AS inet), :userAgent, :correlationId, :createdAt)
                        RETURNING id, security_user_id, application_id, event_type, resource, resource_id,
                                  action, success, description, metadata::text AS metadata,
                                  host(ip_address) AS ip_address, user_agent, correlation_id, created_at
                        """)
                .bind("eventType", audit.getEventType())
                .bind("success", Boolean.TRUE.equals(audit.getSuccess()))
                .bind("createdAt", dateTimeMapper.toOffsetDateTime(
                        audit.getCreatedAt() == null ? Instant.now() : audit.getCreatedAt()));

        spec = SqlBinder.bind(spec, "securityUserId", audit.getSecurityUserId(), Long.class);
        spec = SqlBinder.bind(spec, "applicationId", audit.getApplicationId(), Long.class);
        spec = SqlBinder.bind(spec, "resource", audit.getResource(), String.class);
        spec = SqlBinder.bind(spec, "resourceId", audit.getResourceId(), String.class);
        spec = SqlBinder.bind(spec, "action", audit.getAction(), String.class);
        spec = SqlBinder.bind(spec, "description", audit.getDescription(), String.class);
        spec = SqlBinder.bind(spec, "metadata", audit.getMetadata(), String.class);
        spec = SqlBinder.bind(spec, "ipAddress", audit.getIpAddress(), String.class);
        spec = SqlBinder.bind(spec, "userAgent", audit.getUserAgent(), String.class);
        spec = SqlBinder.bind(spec, "correlationId", audit.getCorrelationId(), String.class);

        return spec.map(this::toModel).one();
    }

    @Override
    public Flux<SecurityAudit> findByUser(Long securityUserId, int limit) {
        return client.sql(SELECT_COLUMNS + """
                         FROM security_audit
                        WHERE security_user_id = :securityUserId
                        ORDER BY created_at DESC
                        LIMIT :limit
                        """)
                .bind("securityUserId", securityUserId)
                .bind("limit", limit)
                .map(this::toModel)
                .all();
    }

    @Override
    public Flux<SecurityAudit> findByApplication(Long applicationId, int limit) {
        return client.sql(SELECT_COLUMNS + """
                         FROM security_audit
                        WHERE application_id = :applicationId
                        ORDER BY created_at DESC
                        LIMIT :limit
                        """)
                .bind("applicationId", applicationId)
                .bind("limit", limit)
                .map(this::toModel)
                .all();
    }

    @Override
    public Flux<SecurityAudit> findByCorrelationId(String correlationId) {
        return client.sql(SELECT_COLUMNS + """
                         FROM security_audit
                        WHERE correlation_id = :correlationId
                        ORDER BY created_at DESC
                        """)
                .bind("correlationId", correlationId)
                .map(this::toModel)
                .all();
    }

    private SecurityAudit toModel(Readable row) {
        return SecurityAudit.builder()
                .id(row.get("id", Long.class))
                .securityUserId(row.get("security_user_id", Long.class))
                .applicationId(row.get("application_id", Long.class))
                .eventType(row.get("event_type", String.class))
                .resource(row.get("resource", String.class))
                .resourceId(row.get("resource_id", String.class))
                .action(row.get("action", String.class))
                .success(row.get("success", Boolean.class))
                .description(row.get("description", String.class))
                .metadata(row.get("metadata", String.class))
                .ipAddress(row.get("ip_address", String.class))
                .userAgent(row.get("user_agent", String.class))
                .correlationId(row.get("correlation_id", String.class))
                .createdAt(dateTimeMapper.toInstant(row.get("created_at", OffsetDateTime.class)))
                .build();
    }
}
