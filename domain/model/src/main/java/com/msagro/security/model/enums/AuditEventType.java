package com.msagro.security.model.enums;

/** Event types written to {@code security_audit}. The trail is append-only. */
public enum AuditEventType {
    USER_REGISTERED,
    LOGIN_SUCCEEDED,
    LOGIN_FAILED,
    ACCOUNT_LOCKED,
    TOKEN_REFRESHED,
    TOKEN_REUSE_DETECTED,
    LOGOUT,
    PASSWORD_CHANGED,
    PASSWORD_RESET_REQUESTED,
    PASSWORD_RESET_COMPLETED,
    ACCESS_GRANTED,
    ACCESS_REVOKED,
    ROLE_ASSIGNED,
    ROLE_REVOKED,
    PERMISSION_GRANTED,
    MASTER_DATA_CREATED
}
