package com.msagro.security.drivenadapters.securitydb.adapter;

import com.msagro.security.drivenadapters.securitydb.mapper.DateTimeMapper;
import com.msagro.security.model.refreshtoken.RefreshToken;
import com.msagro.security.usecase.gateway.securitydb.RefreshTokenRepositoryPort;
import io.r2dbc.spi.Readable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * R2DBC implementation of the refresh-token output port, written with explicit SQL.
 *
 * <p>{@code ip_address} is a PostgreSQL {@code inet} column. Rather than leak a driver-specific
 * type into the entity model, the value is cast on the way in ({@code CAST(:ip AS inet)}) and
 * rendered back to text on the way out ({@code host(ip_address)}), so the domain keeps a plain
 * String and the column keeps its real type — and with it the index behaviour and validation
 * that made {@code inet} worth choosing.</p>
 */
@Component
public class RefreshTokenAdapter implements RefreshTokenRepositoryPort {

    private static final String SELECT_COLUMNS = """
            SELECT id, user_application_id, token_hash, token_family, issued_at, expires_at,
                   revoked_at, replaced_by_token_id, device_id, device_info,
                   host(ip_address) AS ip_address, user_agent
            """;

    private final DatabaseClient client;
    private final DateTimeMapper dateTimeMapper;

    public RefreshTokenAdapter(DatabaseClient client, DateTimeMapper dateTimeMapper) {
        this.client = client;
        this.dateTimeMapper = dateTimeMapper;
    }

    @Override
    public Mono<RefreshToken> save(RefreshToken token) {
        DatabaseClient.GenericExecuteSpec spec = client.sql("""
                        INSERT INTO refresh_token (user_application_id, token_hash, token_family,
                                                   issued_at, expires_at, device_id, device_info,
                                                   ip_address, user_agent)
                        VALUES (:userApplicationId, :tokenHash, :tokenFamily, :issuedAt, :expiresAt,
                                :deviceId, :deviceInfo, CAST(:ipAddress AS inet), :userAgent)
                        RETURNING id, user_application_id, token_hash, token_family, issued_at,
                                  expires_at, revoked_at, replaced_by_token_id, device_id,
                                  device_info, host(ip_address) AS ip_address, user_agent
                        """)
                .bind("userApplicationId", token.getUserApplicationId())
                .bind("tokenHash", token.getTokenHash())
                .bind("tokenFamily", token.getTokenFamily())
                .bind("issuedAt", dateTimeMapper.toOffsetDateTime(
                        token.getIssuedAt() == null ? Instant.now() : token.getIssuedAt()))
                .bind("expiresAt", dateTimeMapper.toOffsetDateTime(token.getExpiresAt()));

        spec = SqlBinder.bind(spec, "deviceId", token.getDeviceId(), String.class);
        spec = SqlBinder.bind(spec, "deviceInfo", token.getDeviceInfo(), String.class);
        spec = SqlBinder.bind(spec, "ipAddress", token.getIpAddress(), String.class);
        spec = SqlBinder.bind(spec, "userAgent", token.getUserAgent(), String.class);

        return spec.map(this::toModel).one();
    }

    @Override
    public Mono<RefreshToken> findByTokenHash(String tokenHash) {
        return client.sql(SELECT_COLUMNS + " FROM refresh_token WHERE token_hash = :tokenHash")
                .bind("tokenHash", tokenHash)
                .map(this::toModel)
                .one();
    }

    @Override
    public Mono<Void> revoke(Long tokenId, Instant when, Long replacedByTokenId) {
        DatabaseClient.GenericExecuteSpec spec = client.sql("""
                        UPDATE refresh_token
                           SET revoked_at = :when,
                               replaced_by_token_id = COALESCE(:replacedBy, replaced_by_token_id)
                         WHERE id = :id AND revoked_at IS NULL
                        """)
                .bind("id", tokenId)
                .bind("when", dateTimeMapper.toOffsetDateTime(when));

        return SqlBinder.bind(spec, "replacedBy", replacedByTokenId, Long.class)
                .fetch().rowsUpdated().then();
    }

    @Override
    public Mono<Void> revokeFamily(String tokenFamily, Instant when) {
        return client.sql("""
                        UPDATE refresh_token
                           SET revoked_at = :when
                         WHERE token_family = :tokenFamily AND revoked_at IS NULL
                        """)
                .bind("tokenFamily", tokenFamily)
                .bind("when", dateTimeMapper.toOffsetDateTime(when))
                .fetch().rowsUpdated().then();
    }

    @Override
    public Mono<Void> revokeAllForGrant(Long userApplicationId, Instant when) {
        return client.sql("""
                        UPDATE refresh_token
                           SET revoked_at = :when
                         WHERE user_application_id = :userApplicationId AND revoked_at IS NULL
                        """)
                .bind("userApplicationId", userApplicationId)
                .bind("when", dateTimeMapper.toOffsetDateTime(when))
                .fetch().rowsUpdated().then();
    }

    private RefreshToken toModel(Readable row) {
        return RefreshToken.builder()
                .id(row.get("id", Long.class))
                .userApplicationId(row.get("user_application_id", Long.class))
                .tokenHash(row.get("token_hash", String.class))
                .tokenFamily(row.get("token_family", String.class))
                .issuedAt(dateTimeMapper.toInstant(row.get("issued_at", OffsetDateTime.class)))
                .expiresAt(dateTimeMapper.toInstant(row.get("expires_at", OffsetDateTime.class)))
                .revokedAt(dateTimeMapper.toInstant(row.get("revoked_at", OffsetDateTime.class)))
                .replacedByTokenId(row.get("replaced_by_token_id", Long.class))
                .deviceId(row.get("device_id", String.class))
                .deviceInfo(row.get("device_info", String.class))
                .ipAddress(row.get("ip_address", String.class))
                .userAgent(row.get("user_agent", String.class))
                .build();
    }
}
