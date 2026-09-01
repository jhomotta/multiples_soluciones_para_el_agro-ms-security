package com.msagro.security.usecase.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT configuration, bound from the {@code security.jwt} namespace. Shared by the use-case
 * layer (refresh-token lifetime) and the security-jwt adapter (signing secret, issuer,
 * access-token lifetime).
 */
@Data
@Component
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /** Base64-encoded HMAC secret; at least 32 bytes after decoding, for HS256. */
    private String secret;

    /** Issuer claim placed in — and required on — every access token. */
    private String issuer = "ms-security";

    /** Access-token lifetime in minutes. Short on purpose: revocation relies on it. */
    private long accessTokenMinutes = 15;

    /** Refresh-token lifetime in days. */
    private long refreshTokenDays = 7;
}
