package com.msagro.security.usecase;

import com.msagro.security.model.auth.ChangePasswordRequest;
import com.msagro.security.model.auth.ClientContext;
import com.msagro.security.model.enums.AuditEventType;
import com.msagro.security.model.error.AuthenticationException;
import com.msagro.security.model.error.BusinessException;
import com.msagro.security.model.error.NotFoundException;
import com.msagro.security.usecase.business.ChangePasswordUseCase;
import com.msagro.security.usecase.gateway.security.PasswordHasherPort;
import com.msagro.security.usecase.gateway.securitydb.RefreshTokenRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.SecurityUserRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.UserApplicationRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Change-password use case for an authenticated user.
 *
 * <p>Requires the current password — an access token alone is not enough, because a stolen token
 * must not be enough to take the account over. On success the password hash is replaced,
 * {@code security_stamp} is bumped, and every refresh token of every application the user can
 * reach is revoked, so all other sessions die.</p>
 */
@Service
public class ChangePasswordUseCaseImpl implements ChangePasswordUseCase {

    private final SecurityUserRepositoryPort userRepository;
    private final UserApplicationRepositoryPort userApplicationRepository;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final PasswordHasherPort passwordHasher;
    private final AuditRecorder auditRecorder;

    public ChangePasswordUseCaseImpl(SecurityUserRepositoryPort userRepository,
                                     UserApplicationRepositoryPort userApplicationRepository,
                                     RefreshTokenRepositoryPort refreshTokenRepository,
                                     PasswordHasherPort passwordHasher,
                                     AuditRecorder auditRecorder) {
        this.userRepository = userRepository;
        this.userApplicationRepository = userApplicationRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
        this.auditRecorder = auditRecorder;
    }

    @Override
    @Transactional
    public Mono<Void> changePassword(Long securityUserId,
                                     ChangePasswordRequest request,
                                     ClientContext context) {
        Instant now = Instant.now();

        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            return Mono.error(new BusinessException("The new password must differ from the current one"));
        }

        return userRepository.findById(securityUserId)
                .switchIfEmpty(Mono.error(new NotFoundException("SecurityUser", securityUserId)))
                .flatMap(user -> passwordHasher.matches(request.getCurrentPassword(), user.getPasswordHash())
                        .flatMap(matches -> Boolean.TRUE.equals(matches)
                                ? Mono.just(user)
                                : Mono.error(new AuthenticationException("Current password is not correct"))))
                .flatMap(user -> passwordHasher.hash(request.getNewPassword())
                        .flatMap(newHash -> userRepository.updatePassword(user.getId(), newHash, now)))
                .then(revokeEverySession(securityUserId, now))
                .then(auditRecorder.record(AuditEventType.PASSWORD_CHANGED, securityUserId, null, true,
                        "Password changed; all sessions revoked", context));
    }

    /** A password change ends every session the user holds, in every application. */
    private Mono<Void> revokeEverySession(Long securityUserId, Instant now) {
        return userApplicationRepository.findByUser(securityUserId)
                .flatMap(grant -> refreshTokenRepository.revokeAllForGrant(grant.getId(), now))
                .then();
    }
}
