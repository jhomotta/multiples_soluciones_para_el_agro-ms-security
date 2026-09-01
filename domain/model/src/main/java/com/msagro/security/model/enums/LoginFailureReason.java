package com.msagro.security.model.enums;

/**
 * Why an authentication attempt failed. Stored in {@code login_attempt.failure_reason}
 * for forensics — it is never returned to the caller, which always sees the same
 * generic "invalid credentials" message.
 */
public enum LoginFailureReason {
    UNKNOWN_USERNAME,
    BAD_PASSWORD,
    ACCOUNT_DISABLED,
    ACCOUNT_LOCKED,
    ACCOUNT_EXPIRED,
    CREDENTIALS_EXPIRED,
    NO_APPLICATION_ACCESS,
    ACCESS_WINDOW_CLOSED,
    APPLICATION_INACTIVE
}
