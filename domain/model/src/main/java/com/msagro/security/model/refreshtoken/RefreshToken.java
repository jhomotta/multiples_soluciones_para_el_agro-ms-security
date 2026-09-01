package com.msagro.security.model.refreshtoken;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * A stored refresh token bound to one access grant. The raw value never reaches the database:
 * only its SHA-256 hash is kept. Every rotation of one login shares a {@code tokenFamily},
 * so presenting a revoked token can revoke the whole family. Table {@code refresh_token}.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    private Long id;
    private Long userApplicationId;
    private String tokenHash;
    private String tokenFamily;
    private Instant issuedAt;
    private Instant expiresAt;
    private Instant revokedAt;
    private Long replacedByTokenId;
    private String deviceId;
    private String deviceInfo;
    private String ipAddress;
    private String userAgent;

    /** A token can be exchanged only while it is neither revoked nor expired. */
    public boolean isUsableAt(Instant now) {
        return revokedAt == null && expiresAt != null && expiresAt.isAfter(now);
    }
}
