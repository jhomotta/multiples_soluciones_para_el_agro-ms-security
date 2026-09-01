package com.msagro.security.usecase;

import com.msagro.security.model.auth.ClientContext;
import com.msagro.security.model.auth.PasswordResetConfirmRequest;
import com.msagro.security.model.auth.PasswordResetRequest;
import com.msagro.security.model.auth.PasswordResetTicket;
import com.msagro.security.model.enums.AuditEventType;
import com.msagro.security.model.error.TokenException;
import com.msagro.security.model.passwordresettoken.PasswordResetToken;
import com.msagro.security.usecase.business.PasswordResetUseCase;
import com.msagro.security.usecase.config.SecurityPolicyProperties;
import com.msagro.security.usecase.gateway.security.PasswordHasherPort;
import com.msagro.security.usecase.gateway.security.SecureTokenGeneratorPort;
import com.msagro.security.usecase.gateway.security.TokenHasherPort;
import com.msagro.security.usecase.gateway.securitydb.PasswordResetTokenRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.RefreshTokenRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.SecurityUserRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.UserApplicationRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Self-service password reset, in two halves.
 *
 * <p><b>Request.</b> Always answers the same thing, whether or not the username exists and
 * whether or not it can reach the application — otherwise the endpoint would be a free account
 * enumeration oracle. When the user does exist, any outstanding token is invalidated first, so
 * only the newest link ever works.</p>
 *
 * <p><b>Confirm.</b> The token is single-use and short-lived. Consuming it replaces the password,
 * bumps {@code security_stamp} and revokes every refresh token of the grant.</p>
 *
 * <p>The raw token is returned in the response only when
 * {@code security.policy.expose-reset-token} is on — a local-testing switch. In production the
 * token belongs in an e-mail, and the field stays null.</p>
 */
@Service
public class PasswordResetUseCaseImpl implements PasswordResetUseCase {

    private static final String NEUTRAL_ANSWER =
            "If the account exists, a password reset token has been issued.";

    private final SecurityUserRepositoryPort userRepository;
    private final UserApplicationRepositoryPort userApplicationRepository;
    private final PasswordResetTokenRepositoryPort resetTokenRepository;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final SecureTokenGeneratorPort tokenGenerator;
    private final TokenHasherPort tokenHasher;
    private final PasswordHasherPort passwordHasher;
    private final SecurityPolicyProperties policy;
    private final AuditRecorder auditRecorder;

    public PasswordResetUseCaseImpl(SecurityUserRepositoryPort userRepository,
                                    UserApplicationRepositoryPort userApplicationRepository,
                                    PasswordResetTokenRepositoryPort resetTokenRepository,
                                    RefreshTokenRepositoryPort refreshTokenRepository,
                                    SecureTokenGeneratorPort tokenGenerator,
                                    TokenHasherPort tokenHasher,
                                    PasswordHasherPort passwordHasher,
                                    SecurityPolicyProperties policy,
                                    AuditRecorder auditRecorder) {
        this.userRepository = userRepository;
        this.userApplicationRepository = userApplicationRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.tokenHasher = tokenHasher;
        this.passwordHasher = passwordHasher;
        this.policy = policy;
        this.auditRecorder = auditRecorder;
    }

    @Override
    @Transactional
    public Mono<PasswordResetTicket> request(PasswordResetRequest request, ClientContext context) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(policy.getPasswordResetMinutes()));

        return userRepository.findByUsernameIgnoreCase(request.getUsername())
                .filter(user -> Boolean.TRUE.equals(user.getEnabled()))
                .flatMap(user -> userApplicationRepository
                        .findByUserAndApplication(user.getId(), request.getApplicationId())
                        .filter(grant -> grant.isUsableAt(now))
                        .flatMap(grant -> issueResetToken(grant.getId(), expiresAt, now, context)
                                .flatMap(rawToken -> auditRecorder.record(
                                                AuditEventType.PASSWORD_RESET_REQUESTED, user.getId(),
                                                grant.getApplicationId(), true,
                                                "Password reset token issued", context)
                                        .thenReturn(ticket(rawToken, expiresAt)))))
                // Unknown user, disabled account or no access: same answer, no token.
                .defaultIfEmpty(ticket(null, null));
    }

    @Override
    @Transactional
    public Mono<Void> confirm(PasswordResetConfirmRequest request, ClientContext context) {
        Instant now = Instant.now();
        String presentedHash = tokenHasher.hash(request.getResetToken());

        return resetTokenRepository.findByTokenHash(presentedHash)
                .switchIfEmpty(Mono.error(new TokenException("Invalid password reset token")))
                .flatMap(token -> token.isUsableAt(now)
                        ? Mono.just(token)
                        : Mono.error(new TokenException("Password reset token is expired or already used")))
                .flatMap(token -> applyNewPassword(token, request.getNewPassword(), now, context));
    }

    // ── steps ────────────────────────────────────────────────────────────────

    private Mono<String> issueResetToken(Long grantId,
                                         Instant expiresAt,
                                         Instant now,
                                         ClientContext context) {
        ClientContext ctx = context == null ? ClientContext.empty() : context;
        String rawToken = tokenGenerator.generateToken();

        return resetTokenRepository.invalidateOutstanding(grantId, now)
                .then(resetTokenRepository.save(PasswordResetToken.builder()
                        .userApplicationId(grantId)
                        .tokenHash(tokenHasher.hash(rawToken))
                        .requestedAt(now)
                        .expiresAt(expiresAt)
                        .ipAddress(ctx.getIpAddress())
                        .userAgent(ctx.getUserAgent())
                        .build()))
                .thenReturn(rawToken);
    }

    private Mono<Void> applyNewPassword(PasswordResetToken token,
                                        String newPassword,
                                        Instant now,
                                        ClientContext context) {
        return userApplicationRepository.findById(token.getUserApplicationId())
                .switchIfEmpty(Mono.error(new TokenException("Invalid password reset token")))
                .flatMap(grant -> passwordHasher.hash(newPassword)
                        .flatMap(newHash -> userRepository
                                .updatePassword(grant.getSecurityUserId(), newHash, now))
                        .then(resetTokenRepository.markUsed(token.getId(), now))
                        .then(refreshTokenRepository.revokeAllForGrant(grant.getId(), now))
                        .then(auditRecorder.record(AuditEventType.PASSWORD_RESET_COMPLETED,
                                grant.getSecurityUserId(), grant.getApplicationId(), true,
                                "Password reset completed; all sessions of the grant revoked", context)));
    }

    private PasswordResetTicket ticket(String rawToken, Instant expiresAt) {
        boolean expose = policy.isExposeResetToken() && rawToken != null;
        return PasswordResetTicket.builder()
                .message(NEUTRAL_ANSWER)
                .resetToken(expose ? rawToken : null)
                .expiresAt(expose ? expiresAt : null)
                .build();
    }
}
