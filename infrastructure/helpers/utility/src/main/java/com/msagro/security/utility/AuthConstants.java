package com.msagro.security.utility;

/** Shared cross-cutting constants of the security microservice. */
public final class AuthConstants {

    private AuthConstants() {
    }

    /** Prefix of the {@code Authorization} header value that carries the access token. */
    public static final String BEARER_PREFIX = "Bearer ";

    /** Authority prefix Spring Security uses for roles. */
    public static final String ROLE_PREFIX = "ROLE_";

    /** Header carrying a caller-supplied trace id; echoed into {@code security_audit}. */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    /** Header carrying a stable device identifier, stored with the refresh token. */
    public static final String DEVICE_ID_HEADER = "X-Device-Id";

    /** Header set by a reverse proxy with the original client address. */
    public static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
}
