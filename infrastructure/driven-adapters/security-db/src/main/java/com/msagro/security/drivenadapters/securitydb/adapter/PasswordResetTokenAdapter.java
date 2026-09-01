package com.msagro.security.drivenadapters.securitydb.adapter;

import com.msagro.security.drivenadapters.securitydb.mapper.DateTimeMapper;
import com.msagro.security.model.passwordresettoken.PasswordResetToken;
import com.msagro.security.usecase.gateway.securitydb.PasswordResetTokenRepositoryPort;
import io.r2dbc.spi.Readable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;

/** R2DBC implementation of the password-reset-token output port. Only hashes are stored. */
@Component
public class PasswordResetTokenAdapter implements PasswordResetTokenRepositoryPort {

    private static final String SELECT_COLUMNS = """
            SELECT id, user_application_id, token_hash, requested_at, expires_at, used_at,
                   invalidated_at, host(ip_address) AS ip_address, user_agent
            """;

    private final DatabaseClient client;
    private final DateTimeMapper dateTimeMapper;

    public PasswordResetTokenAdapter(DatabaseClient client, DateTimeMapper dateTimeMapper) {
        this.client = client;
        this.dateTimeMapper = dateTimeMapper;
    }

    @Override
    public Mono<PasswordResetToken> save(PasswordResetToken token) {
        DatabaseClient.GenericExecuteSpec spec = client.sql("""
                        INSERT INTO password_reset_token (user_application_id, token_hash, requested_at,
                                                          expires_at, ip_address, user_agent)
                        VALUES (:userApplicationId, :tokenHash, :requestedAt, :expiresAt,
                                CAST(:ipAddress AS inet), :userAgent)
                        RETURNING id, user_application_id, token_hash, requested_at, expires_at,
                                  used_at, invalidated_at, host(ip_address) AS ip_address, user_agent
                        """)
                .bind("userApplicationId", token.getUserApplicationId())
                .bind("tokenHash", token.getTokenHash())
                .bind("requestedAt", dateTimeMapper.toOffsetDateTime(
                        token.getRequestedAt() == null ? Instant.now() : token.getRequestedAt()))
                .bind("expiresAt", dateTimeMapper.toOffsetDateTime(token.getExpiresAt()));

        spec = SqlBinder.bind(spec, "ipAddress", token.getIpAddress(), String.class);
        spec = SqlBinder.bind(spec, "userAgent", token.getUserAgent(), String.class);

        return spec.map(this::toModel).one();
    }

    @Override
    public Mono<PasswordResetToken> findByTokenHash(String tokenHash) {
        return client.sql(SELECT_COLUMNS + " FROM password_reset_token WHERE token_hash = :tokenHash")
                .bind("tokenHash", tokenHash)
                .map(this::toModel)
                .one();
    }

    @Override
    public Mono<Void> markUsed(Long id, Instant when) {
        return client.sql("UPDATE password_reset_token SET used_at = :when WHERE id = :id AND used_at IS NULL")
                .bind("id", id)
                .bind("when", dateTimeMapper.toOffsetDateTime(when))
                .fetch().rowsUpdated().then();
    }

    @Override
    public Mono<Void> invalidateOutstanding(Long userApplicationId, Instant when) {
        return client.sql("""
                        UPDATE password_reset_token
                           SET invalidated_at = :when
                         WHERE user_application_id = :userApplicationId
                           AND used_at IS NULL
                           AND invalidated_at IS NULL
                        """)
                .bind("userApplicationId", userApplicationId)
                .bind("when", dateTimeMapper.toOffsetDateTime(when))
                .fetch().rowsUpdated().then();
    }

    private PasswordResetToken toModel(Readable row) {
        return PasswordResetToken.builder()
                .id(row.get("id", Long.class))
                .userApplicationId(row.get("user_application_id", Long.class))
                .tokenHash(row.get("token_hash", String.class))
                .requestedAt(dateTimeMapper.toInstant(row.get("requested_at", OffsetDateTime.class)))
                .expiresAt(dateTimeMapper.toInstant(row.get("expires_at", OffsetDateTime.class)))
                .usedAt(dateTimeMapper.toInstant(row.get("used_at", OffsetDateTime.class)))
                .invalidatedAt(dateTimeMapper.toInstant(row.get("invalidated_at", OffsetDateTime.class)))
                .ipAddress(row.get("ip_address", String.class))
                .userAgent(row.get("user_agent", String.class))
                .build();
    }
}
