package com.msagro.security.model.passwordresettoken;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Single-use, short-lived password reset token, stored only as a SHA-256 hash.
 * Table {@code password_reset_token}.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {
    private Long id;
    private Long userApplicationId;
    private String tokenHash;
    private Instant requestedAt;
    private Instant expiresAt;
    private Instant usedAt;
    private Instant invalidatedAt;
    private String ipAddress;
    private String userAgent;

    /** Usable only while unused, not invalidated and not expired. */
    public boolean isUsableAt(Instant now) {
        return usedAt == null
                && invalidatedAt == null
                && expiresAt != null
                && expiresAt.isAfter(now);
    }
}
