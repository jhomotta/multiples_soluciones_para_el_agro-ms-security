package com.msagro.security.drivenadapters.securityjwt;

import com.msagro.security.usecase.gateway.security.TokenHasherPort;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 hashing for opaque tokens.
 *
 * <p>Unlike a password, a refresh or reset token has to be found <em>by</em> its hash, so the
 * hash must be deterministic — a salted, slow hash would make the lookup impossible. That is
 * safe here for the reason it would not be for a password: these tokens are 256 bits of output
 * from a CSPRNG, so there is no dictionary to run against them.</p>
 */
@Component
public class Sha256TokenHasherAdapter implements TokenHasherPort {

    @Override
    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
