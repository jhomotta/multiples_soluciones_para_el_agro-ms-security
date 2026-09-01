package com.msagro.security.usecase.gateway.security;

import reactor.core.publisher.Mono;

/**
 * Output port for password hashing. The implementation uses a {@code DelegatingPasswordEncoder}
 * whose default algorithm is Argon2id; older BCrypt hashes stay verifiable so they can be
 * upgraded on the next successful login. Hashing is CPU-bound, so both methods return a Mono
 * and run off the event loop.
 */
public interface PasswordHasherPort {

    Mono<String> hash(String rawPassword);

    Mono<Boolean> matches(String rawPassword, String passwordHash);

    /** True when the stored hash uses an algorithm or parameters that should be re-hashed. */
    boolean needsUpgrade(String passwordHash);
}
