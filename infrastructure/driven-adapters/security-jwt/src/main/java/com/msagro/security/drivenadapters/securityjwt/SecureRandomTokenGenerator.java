package com.msagro.security.drivenadapters.securityjwt;

import com.msagro.security.usecase.gateway.security.SecureTokenGeneratorPort;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Opaque secrets from {@link SecureRandom}: 256-bit refresh and reset tokens, and 128-bit
 * token-family identifiers.
 *
 * <p>The family id is random rather than sequential on purpose — it is stored next to the token
 * and appears in audit records, and a predictable family id would let an attacker who sees one
 * value reason about the others.</p>
 */
@Component
public class SecureRandomTokenGenerator implements SecureTokenGeneratorPort {

    private static final int TOKEN_BYTES = 32;
    private static final int FAMILY_BYTES = 16;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    @Override
    public String generateToken() {
        return randomString(TOKEN_BYTES);
    }

    @Override
    public String generateTokenFamily() {
        return randomString(FAMILY_BYTES);
    }

    private String randomString(int byteCount) {
        byte[] bytes = new byte[byteCount];
        secureRandom.nextBytes(bytes);
        return encoder.encodeToString(bytes);
    }
}
