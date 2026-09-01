package com.msagro.security.model.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Technical context of the caller, filled in by the web layer and carried into the use cases
 * so they can write meaningful {@code login_attempt}, {@code refresh_token} and
 * {@code security_audit} rows. Never supplied by the client body.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientContext {

    private String ipAddress;
    private String userAgent;
    private String deviceId;
    private String deviceInfo;
    private String correlationId;

    /** An empty context, for calls made outside an HTTP request. */
    public static ClientContext empty() {
        return ClientContext.builder().build();
    }
}
