package com.msagro.security.usecase;

import com.msagro.security.model.auth.AuthResponse;
import com.msagro.security.model.auth.ClientContext;
import com.msagro.security.model.auth.LoginRequest;
import com.msagro.security.model.enums.AuditEventType;
import com.msagro.security.model.enums.LoginFailureReason;
import com.msagro.security.model.error.AuthenticationException;
import com.msagro.security.model.loginattempt.LoginAttempt;
import com.msagro.security.model.securityuser.SecurityUser;
import com.msagro.security.model.userapplication.UserApplication;
import com.msagro.security.usecase.business.AuthenticateUseCase;
import com.msagro.security.usecase.config.SecurityPolicyProperties;
import com.msagro.security.usecase.gateway.security.PasswordHasherPort;
import com.msagro.security.usecase.gateway.securitydb.ApplicationRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.LoginAttemptRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.SecurityUserRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.UserApplicationRepositoryPort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Login use case.
 *
 * <p>A login is scoped to an application: valid credentials are not enough, the user also needs
 * an active {@code user_application} grant whose access window is open. Every attempt — including
 * attempts against usernames that do not exist — is written to {@code login_attempt} with a
 * precise {@code failure_reason}, while the caller always receives the same generic message so
 * the endpoint cannot be used to enumerate accounts or probe account state.</p>
 *
 * <p>Consecutive failures lock the account for a configurable window. A successful login clears
 * the counter and, when the stored hash uses an outdated algorithm, transparently re-hashes the
 * password with the current one.</p>
 */
@Service
public class AuthenticateUseCaseImpl implements AuthenticateUseCase {

    /** Returned for every failure, whatever the real reason was. */
    private static final String GENERIC_FAILURE = "Invalid credentials";

    private final SecurityUserRepositoryPort userRepository;
    private final UserApplicationRepositoryPort userApplicationRepository;
    private final ApplicationRepositoryPort applicationRepository;
    private final LoginAttemptRepositoryPort loginAttemptRepository;
    private final PasswordHasherPort passwordHasher;
    private final SecurityPolicyProperties policy;
    private final TokenIssuer tokenIssuer;
    private final AuditRecorder auditRecorder;

    public AuthenticateUseCaseImpl(SecurityUserRepositoryPort userRepository,
                                   UserApplicationRepositoryPort userApplicationRepository,
                                   ApplicationRepositoryPort applicationRepository,
                                   LoginAttemptRepositoryPort loginAttemptRepository,
                                   PasswordHasherPort passwordHasher,
                                   SecurityPolicyProperties policy,
                                   TokenIssuer tokenIssuer,
                                   AuditRecorder auditRecorder) {
        this.userRepository = userRepository;
        this.userApplicationRepository = userApplicationRepository;
        this.applicationRepository = applicationRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.passwordHasher = passwordHasher;
        this.policy = policy;
        this.tokenIssuer = tokenIssuer;
        this.auditRecorder = auditRecorder;
    }

    @Override
    public Mono<AuthResponse> login(LoginRequest request, ClientContext context) {
        Instant now = Instant.now();
        ClientContext ctx = context == null ? ClientContext.empty() : context;

        return userRepository.findByUsernameIgnoreCase(request.getUsername())
                .switchIfEmpty(fail(request, ctx, null, LoginFailureReason.UNKNOWN_USERNAME))
                .flatMap(user -> checkAccountState(user, request, ctx, now))
                .flatMap(user -> verifyPassword(user, request, ctx, now))
                .flatMap(user -> resolveGrant(user, request, ctx, now))
                .flatMap(state -> succeed(state, request, ctx, now));
    }

    // ── steps ────────────────────────────────────────────────────────────────

    /** Enabled, not locked, not expired, credentials not expired. */
    private Mono<SecurityUser> checkAccountState(SecurityUser user,
                                                 LoginRequest request,
                                                 ClientContext ctx,
                                                 Instant now) {
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            return fail(request, ctx, user.getId(), LoginFailureReason.ACCOUNT_DISABLED);
        }
        if (user.isLockedAt(now)) {
            return fail(request, ctx, user.getId(), LoginFailureReason.ACCOUNT_LOCKED);
        }
        if (user.isAccountExpiredAt(now)) {
            return fail(request, ctx, user.getId(), LoginFailureReason.ACCOUNT_EXPIRED);
        }
        if (Boolean.TRUE.equals(user.getCredentialsExpired())) {
            return fail(request, ctx, user.getId(), LoginFailureReason.CREDENTIALS_EXPIRED);
        }
        return Mono.just(user);
    }

    private Mono<SecurityUser> verifyPassword(SecurityUser user,
                                              LoginRequest request,
                                              ClientContext ctx,
                                              Instant now) {
        return passwordHasher.matches(request.getPassword(), user.getPasswordHash())
                .flatMap(matches -> Boolean.TRUE.equals(matches)
                        ? Mono.just(user)
                        : onBadPassword(user, request, ctx, now));
    }

    /** Counts the failure and locks the account once the threshold is reached. */
    private Mono<SecurityUser> onBadPassword(SecurityUser user,
                                             LoginRequest request,
                                             ClientContext ctx,
                                             Instant now) {
        return userRepository.registerFailedAttempt(user.getId())
                .flatMap(attempts -> {
                    if (attempts < policy.getMaxFailedAttempts()) {
                        return Mono.<Void>empty();
                    }
                    Instant until = now.plus(Duration.ofMinutes(policy.getLockMinutes()));
                    return userRepository.lockUntil(user.getId(), until)
                            .then(auditRecorder.record(AuditEventType.ACCOUNT_LOCKED, user.getId(),
                                    request.getApplicationId(), false,
                                    "Account locked after " + attempts + " consecutive failed attempts", ctx));
                })
                .then(fail(request, ctx, user.getId(), LoginFailureReason.BAD_PASSWORD));
    }

    /** The credentials are right; now check that this user may enter this application. */
    private Mono<LoginState> resolveGrant(SecurityUser user,
                                          LoginRequest request,
                                          ClientContext ctx,
                                          Instant now) {
        return applicationRepository.findById(request.getApplicationId())
                .filter(application -> Boolean.TRUE.equals(application.getActive()))
                .switchIfEmpty(fail(request, ctx, user.getId(), LoginFailureReason.APPLICATION_INACTIVE))
                .then(userApplicationRepository
                        .findByUserAndApplication(user.getId(), request.getApplicationId()))
                .switchIfEmpty(fail(request, ctx, user.getId(), LoginFailureReason.NO_APPLICATION_ACCESS))
                .flatMap(grant -> grant.isUsableAt(now)
                        ? Mono.just(new LoginState(user, grant))
                        : fail(request, ctx, user.getId(), LoginFailureReason.ACCESS_WINDOW_CLOSED));
    }

    private Mono<AuthResponse> succeed(LoginState state,
                                       LoginRequest request,
                                       ClientContext ctx,
                                       Instant now) {
        SecurityUser user = state.user();

        return upgradeHashIfNeeded(user, request.getPassword())
                .then(userRepository.registerSuccessfulLogin(user.getId(), now))
                .then(loginAttemptRepository.save(attempt(request, ctx, user.getId(), true, null)))
                .then(auditRecorder.record(AuditEventType.LOGIN_SUCCEEDED, user.getId(),
                        state.grant().getApplicationId(), true,
                        "Successful login for application " + state.grant().getApplicationId(), ctx))
                .then(tokenIssuer.issueNewSession(user, state.grant(), withDevice(ctx, request)));
    }

    /**
     * Re-hashes the password with the current algorithm when the stored hash is outdated
     * (for example a legacy BCrypt hash). This is the only moment the raw password is available.
     */
    private Mono<Void> upgradeHashIfNeeded(SecurityUser user, String rawPassword) {
        if (!passwordHasher.needsUpgrade(user.getPasswordHash())) {
            return Mono.empty();
        }
        return passwordHasher.hash(rawPassword)
                .flatMap(newHash -> userRepository.updatePassword(user.getId(), newHash, Instant.now()))
                // An upgrade failure must never block an otherwise valid login.
                .onErrorResume(error -> Mono.empty());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Records the failed attempt, then fails with the same generic message every time. */
    private <T> Mono<T> fail(LoginRequest request,
                             ClientContext ctx,
                             Long userId,
                             LoginFailureReason reason) {
        return Mono.defer(() -> loginAttemptRepository
                .save(attempt(request, ctx, userId, false, reason))
                .then(auditRecorder.record(AuditEventType.LOGIN_FAILED, userId,
                        request.getApplicationId(), false, "Login failed: " + reason.name(), ctx))
                .then(Mono.error(new AuthenticationException(GENERIC_FAILURE))));
    }

    private LoginAttempt attempt(LoginRequest request,
                                 ClientContext ctx,
                                 Long userId,
                                 boolean success,
                                 LoginFailureReason reason) {
        return LoginAttempt.builder()
                .securityUserId(userId)
                .applicationId(request.getApplicationId())
                .usernameAttempted(request.getUsername())
                .success(success)
                .ipAddress(ctx.getIpAddress())
                .userAgent(ctx.getUserAgent())
                .failureReason(reason == null ? null : reason.name())
                .attemptedAt(Instant.now())
                .build();
    }

    /** Carries the device id from the login body into the refresh token that is about to be stored. */
    private ClientContext withDevice(ClientContext ctx, LoginRequest request) {
        if (request.getDeviceId() == null || request.getDeviceId().isBlank()) {
            return ctx;
        }
        return ClientContext.builder()
                .ipAddress(ctx.getIpAddress())
                .userAgent(ctx.getUserAgent())
                .deviceId(request.getDeviceId())
                .deviceInfo(ctx.getDeviceInfo())
                .correlationId(ctx.getCorrelationId())
                .build();
    }

    /** The user and the grant that survived every check. */
    private record LoginState(SecurityUser user, UserApplication grant) {
    }
}
