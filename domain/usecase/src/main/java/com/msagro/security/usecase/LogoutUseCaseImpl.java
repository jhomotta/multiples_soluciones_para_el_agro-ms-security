package com.msagro.security.usecase;

import com.msagro.security.model.auth.ClientContext;
import com.msagro.security.model.auth.LogoutRequest;
import com.msagro.security.model.enums.AuditEventType;
import com.msagro.security.usecase.business.LogoutUseCase;
import com.msagro.security.usecase.gateway.security.TokenHasherPort;
import com.msagro.security.usecase.gateway.securitydb.RefreshTokenRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.UserApplicationRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Logout use case. Revokes the presented refresh token, or — with {@code allSessions} — every
 * live token of the same access grant.
 *
 * <p>Idempotent and silent: an unknown or already revoked token completes normally. Answering
 * "that token does not exist" would turn logout into an oracle for guessing valid tokens.</p>
 */
@Service
public class LogoutUseCaseImpl implements LogoutUseCase {

    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final UserApplicationRepositoryPort userApplicationRepository;
    private final TokenHasherPort tokenHasher;
    private final AuditRecorder auditRecorder;

    public LogoutUseCaseImpl(RefreshTokenRepositoryPort refreshTokenRepository,
                             UserApplicationRepositoryPort userApplicationRepository,
                             TokenHasherPort tokenHasher,
                             AuditRecorder auditRecorder) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userApplicationRepository = userApplicationRepository;
        this.tokenHasher = tokenHasher;
        this.auditRecorder = auditRecorder;
    }

    @Override
    @Transactional
    public Mono<Void> logout(LogoutRequest request, ClientContext context) {
        Instant now = Instant.now();

        return refreshTokenRepository.findByTokenHash(tokenHasher.hash(request.getRefreshToken()))
                .flatMap(token -> revoke(token.getId(), token.getUserApplicationId(),
                        token.getRevokedAt() != null, request.isAllSessions(), now, context))
                .then();
    }

    private Mono<Void> revoke(Long tokenId,
                              Long grantId,
                              boolean alreadyRevoked,
                              boolean allSessions,
                              Instant now,
                              ClientContext context) {
        Mono<Void> revocation;
        if (allSessions) {
            revocation = refreshTokenRepository.revokeAllForGrant(grantId, now);
        } else if (alreadyRevoked) {
            revocation = Mono.empty();
        } else {
            revocation = refreshTokenRepository.revoke(tokenId, now, null);
        }

        return revocation.then(userApplicationRepository.findById(grantId)
                .flatMap(grant -> auditRecorder.record(AuditEventType.LOGOUT,
                        grant.getSecurityUserId(), grant.getApplicationId(), true,
                        allSessions ? "Logged out of every session" : "Logged out of one session",
                        context))
                .then());
    }
}
