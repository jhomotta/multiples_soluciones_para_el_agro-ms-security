package com.msagro.security.usecase.gateway.securitydb;

import com.msagro.security.model.securityuser.SecurityUser;
import reactor.core.publisher.Mono;

import java.time.Instant;

/** Output port for {@code security_user} credentials and account state. */
public interface SecurityUserRepositoryPort {

    Mono<SecurityUser> save(SecurityUser user);

    Mono<SecurityUser> findById(Long id);

    /** Case-insensitive lookup, matching the {@code LOWER(username)} unique index. */
    Mono<SecurityUser> findByUsernameIgnoreCase(String username);

    Mono<SecurityUser> findByPersonId(Long personId);

    Mono<Boolean> existsByUsernameIgnoreCase(String username);

    /** Clears the failure counter and stamps the successful login. */
    Mono<Void> registerSuccessfulLogin(Long userId, Instant when);

    /** Increments {@code failed_attempts} and returns the new value. */
    Mono<Integer> registerFailedAttempt(Long userId);

    /** Locks the account until the given instant (null means "until an admin unlocks it"). */
    Mono<Void> lockUntil(Long userId, Instant until);

    /**
     * Replaces the password hash, resets the failure counter and flags, and bumps
     * {@code security_stamp} so tokens issued before the change stop being honoured.
     */
    Mono<Void> updatePassword(Long userId, String passwordHash, Instant when);

    /** Bumps {@code security_stamp} alone (used when roles or state change). */
    Mono<Void> bumpSecurityStamp(Long userId);
}
