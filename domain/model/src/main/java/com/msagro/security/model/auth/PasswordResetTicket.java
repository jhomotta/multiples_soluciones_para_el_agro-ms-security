package com.msagro.security.model.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Result of requesting a password reset. In production the raw token is delivered by mail and
 * {@code resetToken} stays null; it is only echoed back when
 * {@code security.password-reset.expose-token} is enabled for local testing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetTicket {
    private String message;
    private String resetToken;
    private Instant expiresAt;
}
