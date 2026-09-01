package com.msagro.security.model.securityuser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Credentials of a person. Named {@code SecurityUser} because {@code user} is a reserved
 * word in PostgreSQL. The password lives only as an Argon2id hash in {@code passwordHash}.
 * Table {@code security_user}.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SecurityUser {
    private Long id;
    private Long personId;
    private String username;
    private String passwordHash;
    private Boolean enabled;
    private Boolean locked;
    private Instant lockedUntil;
    private Integer failedAttempts;
    private Instant lastLoginAt;
    private Instant passwordChangedAt;
    private Boolean credentialsExpired;
    private Instant accountExpiresAt;
    private Boolean mustChangePassword;
    /** Version counter; bumped on password, role or state changes to invalidate sessions. */
    private Long securityStamp;
    private Instant createdAt;
    private Long createdBy;
    private Instant updatedAt;
    private Long updatedBy;

    /** True when a lock is in force at {@code now} (a lock without an end date never lifts). */
    public boolean isLockedAt(Instant now) {
        if (!Boolean.TRUE.equals(locked)) {
            return false;
        }
        return lockedUntil == null || lockedUntil.isAfter(now);
    }

    /** True when the account itself has an expiry date already in the past. */
    public boolean isAccountExpiredAt(Instant now) {
        return accountExpiresAt != null && !accountExpiresAt.isAfter(now);
    }
}
