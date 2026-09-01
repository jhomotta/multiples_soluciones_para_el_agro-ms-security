package com.msagro.security.drivenadapters.securitydb.adapter;

import com.msagro.security.drivenadapters.securitydb.mapper.DateTimeMapper;
import com.msagro.security.model.loginattempt.LoginAttempt;
import com.msagro.security.usecase.gateway.securitydb.LoginAttemptRepositoryPort;
import io.r2dbc.spi.Readable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * R2DBC implementation of the login-attempt output port. Insert-only: attempts are evidence,
 * so nothing here updates or deletes a row. The {@code inet} column is cast explicitly, as in
 * {@link RefreshTokenAdapter}.
 */
@Component
public class LoginAttemptAdapter implements LoginAttemptRepositoryPort {

    private final DatabaseClient client;
    private final DateTimeMapper dateTimeMapper;

    public LoginAttemptAdapter(DatabaseClient client, DateTimeMapper dateTimeMapper) {
        this.client = client;
        this.dateTimeMapper = dateTimeMapper;
    }

    @Override
    public Mono<LoginAttempt> save(LoginAttempt attempt) {
        DatabaseClient.GenericExecuteSpec spec = client.sql("""
                        INSERT INTO login_attempt (security_user_id, application_id, username_attempted,
                                                   success, ip_address, user_agent, failure_reason,
                                                   attempted_at)
                        VALUES (:securityUserId, :applicationId, :usernameAttempted, :success,
                                CAST(:ipAddress AS inet), :userAgent, :failureReason, :attemptedAt)
                        RETURNING id, security_user_id, application_id, username_attempted, success,
                                  host(ip_address) AS ip_address, user_agent, failure_reason, attempted_at
                        """)
                .bind("usernameAttempted", attempt.getUsernameAttempted())
                .bind("success", Boolean.TRUE.equals(attempt.getSuccess()))
                .bind("attemptedAt", dateTimeMapper.toOffsetDateTime(
                        attempt.getAttemptedAt() == null ? Instant.now() : attempt.getAttemptedAt()));

        spec = SqlBinder.bind(spec, "securityUserId", attempt.getSecurityUserId(), Long.class);
        spec = SqlBinder.bind(spec, "applicationId", attempt.getApplicationId(), Long.class);
        spec = SqlBinder.bind(spec, "ipAddress", attempt.getIpAddress(), String.class);
        spec = SqlBinder.bind(spec, "userAgent", attempt.getUserAgent(), String.class);
        spec = SqlBinder.bind(spec, "failureReason", attempt.getFailureReason(), String.class);

        return spec.map(this::toModel).one();
    }

    private LoginAttempt toModel(Readable row) {
        return LoginAttempt.builder()
                .id(row.get("id", Long.class))
                .securityUserId(row.get("security_user_id", Long.class))
                .applicationId(row.get("application_id", Long.class))
                .usernameAttempted(row.get("username_attempted", String.class))
                .success(row.get("success", Boolean.class))
                .ipAddress(row.get("ip_address", String.class))
                .userAgent(row.get("user_agent", String.class))
                .failureReason(row.get("failure_reason", String.class))
                .attemptedAt(dateTimeMapper.toInstant(row.get("attempted_at", OffsetDateTime.class)))
                .build();
    }
}
