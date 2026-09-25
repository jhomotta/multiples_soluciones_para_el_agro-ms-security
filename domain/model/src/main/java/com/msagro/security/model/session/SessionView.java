package com.msagro.security.model.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * One open session: one login and every rotation of its refresh token (one token family).
 * {@code id} is the id of the newest live token of the family; the family itself never leaves
 * the server.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionView {
    private Long id;
    private String deviceId;
    private String deviceInfo;
    private String ipAddress;
    private String userAgent;
    /** When the login happened: the first token of the family. */
    private Instant startedAt;
    /** The last refresh: the newest token of the family. */
    private Instant lastActivityAt;
    private Instant expiresAt;
}
