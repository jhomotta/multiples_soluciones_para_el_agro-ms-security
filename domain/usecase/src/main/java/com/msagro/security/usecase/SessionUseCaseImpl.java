package com.msagro.security.usecase;

import com.msagro.security.model.auth.AuthenticatedPrincipal;
import com.msagro.security.model.auth.ClientContext;
import com.msagro.security.model.enums.AuditEventType;
import com.msagro.security.model.error.NotFoundException;
import com.msagro.security.model.error.TokenException;
import com.msagro.security.model.session.SessionView;
import com.msagro.security.usecase.business.SessionUseCase;
import com.msagro.security.usecase.gateway.securitydb.RefreshTokenRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.SecurityUserRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.UserApplicationRepositoryPort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

/**
 * Sessions. A session is one login: the family of its refresh tokens. The list shows the live
 * token of each family, and closing a session revokes the family.
 */
@Service
public class SessionUseCaseImpl implements SessionUseCase {

    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final SecurityUserRepositoryPort userRepository;
    private final UserApplicationRepositoryPort userApplicationRepository;
    private final AuditRecorder auditRecorder;

    public SessionUseCaseImpl(RefreshTokenRepositoryPort refreshTokenRepository,
                              SecurityUserRepositoryPort userRepository,
                              UserApplicationRepositoryPort userApplicationRepository,
                              AuditRecorder auditRecorder) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.userApplicationRepository = userApplicationRepository;
        this.auditRecorder = auditRecorder;
    }

    @Override
    public Flux<SessionView> listSessions(Long userApplicationId) {
        return refreshTokenRepository.findOpenSessions(userApplicationId, Instant.now());
    }

    @Override
    public Mono<Void> revokeSession(Long userApplicationId, Long sessionId, String reason,
                                    AuthenticatedPrincipal actor, ClientContext context) {
        Instant now = Instant.now();
        return refreshTokenRepository.findById(sessionId)
                // A session of another grant is answered as missing: nothing about it is told.
                .filter(token -> Objects.equals(token.getUserApplicationId(), userApplicationId)
                        && token.getRevokedAt() == null
                        && token.getExpiresAt() != null && token.getExpiresAt().isAfter(now))
                .switchIfEmpty(Mono.error(new NotFoundException("Session", sessionId)))
                .flatMap(token -> refreshTokenRepository.revokeFamily(token.getTokenFamily(), now)
                        .then(userApplicationRepository.findById(userApplicationId))
                        .flatMap(grant -> auditRecorder.record(AuditEventType.SESSION_REVOKED,
                                actor == null ? grant.getSecurityUserId() : actor.getSecurityUserId(),
                                grant.getApplicationId(), true,
                                "Session " + sessionId + " of user " + grant.getSecurityUserId()
                                        + " closed. Reason: " + reason,
                                "SESSION", String.valueOf(sessionId), null, context)));
    }

    @Override
    public Mono<AuthenticatedPrincipal> confirmCurrent(AuthenticatedPrincipal principal) {
        Instant now = Instant.now();
        Mono<Boolean> userStands = userRepository.findById(principal.getSecurityUserId())
                .map(user -> Boolean.TRUE.equals(user.getEnabled())
                        && !user.isLockedAt(now)
                        && Objects.equals(user.getSecurityStamp(), principal.getSecurityStamp()))
                .defaultIfEmpty(false);
        Mono<Boolean> grantStands = userApplicationRepository.findById(principal.getUserApplicationId())
                .map(grant -> grant.isUsableAt(now))
                .defaultIfEmpty(false);
        return Mono.zip(userStands, grantStands)
                .flatMap(t -> t.getT1() && t.getT2()
                        ? Mono.just(principal)
                        : Mono.error(new TokenException("The session is no longer valid")));
    }
}
