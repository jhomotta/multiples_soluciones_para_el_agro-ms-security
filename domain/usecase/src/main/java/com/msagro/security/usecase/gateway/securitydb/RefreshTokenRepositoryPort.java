package com.msagro.security.usecase.gateway.securitydb;

import com.msagro.security.model.refreshtoken.RefreshToken;
import reactor.core.publisher.Mono;

import java.time.Instant;

/** Output port for {@code refresh_token}. Only hashes are ever stored. */
public interface RefreshTokenRepositoryPort {

    Mono<RefreshToken> save(RefreshToken token);

    Mono<RefreshToken> findByTokenHash(String tokenHash);

    /** Marks one token revoked and links it to the token that replaced it (rotation). */
    Mono<Void> revoke(Long tokenId, Instant when, Long replacedByTokenId);

    /** Revokes every live token of a family — the reuse-detection response. */
    Mono<Void> revokeFamily(String tokenFamily, Instant when);

    /** Revokes every live token of an access grant (logout of all sessions). */
    Mono<Void> revokeAllForGrant(Long userApplicationId, Instant when);
}
