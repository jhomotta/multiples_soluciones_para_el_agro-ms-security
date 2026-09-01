package com.msagro.security.usecase;

import com.msagro.security.model.auth.AuthResponse;
import com.msagro.security.model.auth.ClientContext;
import com.msagro.security.model.enums.AuditEventType;
import com.msagro.security.model.error.TokenException;
import com.msagro.security.model.refreshtoken.RefreshToken;
import com.msagro.security.model.securityuser.SecurityUser;
import com.msagro.security.model.userapplication.UserApplication;
import com.msagro.security.usecase.business.RefreshTokenUseCase;
import com.msagro.security.usecase.gateway.security.TokenHasherPort;
import com.msagro.security.usecase.gateway.securitydb.RefreshTokenRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.SecurityUserRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.UserApplicationRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Refresh use case: rotation with family-wide reuse detection.
 *
 * <p>Each refresh revokes the presented token and issues a new one inside the same
 * {@code token_family}, linking old to new through {@code replaced_by_token_id}. Presenting a
 * token that was already revoked means the value leaked, so the entire family is revoked and the
 * legitimate user is forced to log in again — the standard OAuth 2.0 rotation defence.</p>
 *
 * <p>The account and the grant are re-checked on every refresh, so disabling a user or closing an
 * access window takes effect at the next rotation instead of waiting for the refresh token to
 * expire. That window is why the access token is short-lived.</p>
 *
 * <p><b>Why an explicit {@link TransactionalOperator} and not {@code @Transactional}.</b> The two
 * defensive paths here revoke tokens and <em>then</em> fail the call. Under a method-wide
 * transaction the thrown error would roll the revocation back, and a replayed token would keep
 * working — the protection would silently do nothing. So each revocation is committed in its own
 * unit of work and the error is raised after it, while the rotation itself (revoke the old row,
 * insert the new one) stays inside one transaction where atomicity is what matters.</p>
 */
@Service
public class RefreshTokenUseCaseImpl implements RefreshTokenUseCase {

    private static final String INVALID = "Invalid refresh token";

    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final UserApplicationRepositoryPort userApplicationRepository;
    private final SecurityUserRepositoryPort userRepository;
    private final TokenHasherPort tokenHasher;
    private final TokenIssuer tokenIssuer;
    private final AuditRecorder auditRecorder;
    private final TransactionalOperator transactionalOperator;

    public RefreshTokenUseCaseImpl(RefreshTokenRepositoryPort refreshTokenRepository,
                                   UserApplicationRepositoryPort userApplicationRepository,
                                   SecurityUserRepositoryPort userRepository,
                                   TokenHasherPort tokenHasher,
                                   TokenIssuer tokenIssuer,
                                   AuditRecorder auditRecorder,
                                   TransactionalOperator transactionalOperator) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userApplicationRepository = userApplicationRepository;
        this.userRepository = userRepository;
        this.tokenHasher = tokenHasher;
        this.tokenIssuer = tokenIssuer;
        this.auditRecorder = auditRecorder;
        this.transactionalOperator = transactionalOperator;
    }

    @Override
    public Mono<AuthResponse> refresh(String refreshToken, ClientContext context) {
        Instant now = Instant.now();
        String presentedHash = tokenHasher.hash(refreshToken);

        return refreshTokenRepository.findByTokenHash(presentedHash)
                .switchIfEmpty(Mono.error(new TokenException(INVALID)))
                .flatMap(stored -> stored.getRevokedAt() != null
                        ? onReuseDetected(stored, now, context)
                        : validateAndRotate(stored, now, context));
    }

    /**
     * A revoked token came back: the value is in someone else's hands. Revoke the whole family,
     * commit that, and only then refuse. Both the thief and the legitimate holder must log in again.
     */
    private Mono<AuthResponse> onReuseDetected(RefreshToken stored, Instant now, ClientContext context) {
        Mono<Void> revocation = refreshTokenRepository.revokeFamily(stored.getTokenFamily(), now)
                .then(userApplicationRepository.findById(stored.getUserApplicationId()))
                .flatMap(grant -> auditRecorder.record(AuditEventType.TOKEN_REUSE_DETECTED,
                        grant.getSecurityUserId(), grant.getApplicationId(), false,
                        "Revoked refresh token replayed; family " + stored.getTokenFamily()
                                + " fully revoked", context))
                .then();

        return revocation.as(transactionalOperator::transactional)
                .then(Mono.error(new TokenException(
                        "Refresh token already used. Every session of this device family was revoked.")));
    }

    private Mono<AuthResponse> validateAndRotate(RefreshToken stored, Instant now, ClientContext context) {
        if (!stored.isUsableAt(now)) {
            return Mono.error(new TokenException("Refresh token expired"));
        }

        return userApplicationRepository.findById(stored.getUserApplicationId())
                .switchIfEmpty(Mono.error(new TokenException(INVALID)))
                .flatMap(grant -> grant.isUsableAt(now)
                        ? Mono.just(grant)
                        : Mono.error(new TokenException("Access to this application is no longer active")))
                .flatMap(grant -> userRepository.findById(grant.getSecurityUserId())
                        .switchIfEmpty(Mono.error(new TokenException(INVALID)))
                        .flatMap(user -> canAuthenticate(user, now)
                                ? rotate(user, grant, stored, now, context)
                                : lockOutFamily(stored, now)));
    }

    private boolean canAuthenticate(SecurityUser user, Instant now) {
        return Boolean.TRUE.equals(user.getEnabled())
                && !user.isLockedAt(now)
                && !user.isAccountExpiredAt(now);
    }

    /** The account can no longer authenticate: kill the family (committed) and refuse. */
    private Mono<AuthResponse> lockOutFamily(RefreshToken stored, Instant now) {
        return refreshTokenRepository.revokeFamily(stored.getTokenFamily(), now)
                .as(transactionalOperator::transactional)
                .then(Mono.error(new TokenException("Account is not able to authenticate")));
    }

    /** Revoking the old row and inserting the new one is one unit of work. */
    private Mono<AuthResponse> rotate(SecurityUser user,
                                      UserApplication grant,
                                      RefreshToken stored,
                                      Instant now,
                                      ClientContext context) {
        // Carry the device the original token was issued to when the caller sent none.
        ClientContext ctx = inheritDevice(context, stored);

        Mono<AuthResponse> rotation = tokenIssuer.issueWithinFamily(user, grant, stored.getTokenFamily(), ctx)
                .flatMap(response -> refreshTokenRepository
                        .findByTokenHash(tokenHasher.hash(response.getRefreshToken()))
                        .flatMap(issued -> refreshTokenRepository.revoke(stored.getId(), now, issued.getId()))
                        .then(auditRecorder.record(AuditEventType.TOKEN_REFRESHED, user.getId(),
                                grant.getApplicationId(), true, "Refresh token rotated", ctx))
                        .thenReturn(response));

        return rotation.as(transactionalOperator::transactional);
    }

    private ClientContext inheritDevice(ClientContext context, RefreshToken stored) {
        ClientContext ctx = context == null ? ClientContext.empty() : context;
        if (ctx.getDeviceId() != null && !ctx.getDeviceId().isBlank()) {
            return ctx;
        }
        return ClientContext.builder()
                .ipAddress(ctx.getIpAddress())
                .userAgent(ctx.getUserAgent())
                .deviceId(stored.getDeviceId())
                .deviceInfo(stored.getDeviceInfo())
                .correlationId(ctx.getCorrelationId())
                .build();
    }
}
