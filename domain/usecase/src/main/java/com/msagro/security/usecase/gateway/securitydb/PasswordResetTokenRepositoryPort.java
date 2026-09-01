package com.msagro.security.usecase.gateway.securitydb;

import com.msagro.security.model.passwordresettoken.PasswordResetToken;
import reactor.core.publisher.Mono;

import java.time.Instant;

/** Output port for {@code password_reset_token}. Only hashes are ever stored. */
public interface PasswordResetTokenRepositoryPort {

    Mono<PasswordResetToken> save(PasswordResetToken token);

    Mono<PasswordResetToken> findByTokenHash(String tokenHash);

    /** Marks one token as consumed. */
    Mono<Void> markUsed(Long id, Instant when);

    /** Invalidates every outstanding token of a grant (a new request supersedes the old ones). */
    Mono<Void> invalidateOutstanding(Long userApplicationId, Instant when);
}
