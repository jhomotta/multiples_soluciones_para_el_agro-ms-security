package com.msagro.security.usecase.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Account and token policy, bound from the {@code security.policy} namespace.
 * Everything that a security officer may want to tune without touching code lives here.
 */
@Data
@Component
@ConfigurationProperties(prefix = "security.policy")
public class SecurityPolicyProperties {

    /** Consecutive failed logins that lock the account. */
    private int maxFailedAttempts = 5;

    /** How long a lock lasts, in minutes. */
    private long lockMinutes = 15;

    /** Lifetime of a password reset token, in minutes. Deliberately short. */
    private long passwordResetMinutes = 30;

    /**
     * Echo the raw reset token in the API response. For local testing only: in production the
     * token must travel by e-mail and never through the API.
     */
    private boolean exposeResetToken = false;

    /** Role code assigned to a self-registered user, when it exists in the application. */
    private String defaultRoleCode = "USER";

    /** Force {@code must_change_password} on self-registration. */
    private boolean requirePasswordChangeOnRegister = false;
}
