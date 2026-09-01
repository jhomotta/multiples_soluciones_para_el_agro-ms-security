package com.msagro.security.usecase.gateway.security;

/**
 * Output port that creates opaque secrets: refresh tokens, password reset tokens and the
 * non-sequential {@code token_family} identifier. Values are long, random and unguessable.
 */
public interface SecureTokenGeneratorPort {

    /** A 256-bit URL-safe random token. Returned to the client once; only its hash is stored. */
    String generateToken();

    /** A random family identifier for a refresh-token chain (fits {@code VARCHAR(64)}). */
    String generateTokenFamily();
}
