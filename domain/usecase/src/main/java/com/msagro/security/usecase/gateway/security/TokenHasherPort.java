package com.msagro.security.usecase.gateway.security;

/**
 * Output port for hashing opaque tokens before they are stored or looked up. The hash must be
 * deterministic (SHA-256), because a token is found by its hash. The tokens are already long
 * and random, so no salt or key stretching is needed.
 */
public interface TokenHasherPort {

    String hash(String rawToken);
}
