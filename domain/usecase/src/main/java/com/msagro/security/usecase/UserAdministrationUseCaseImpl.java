package com.msagro.security.usecase;

import com.msagro.security.model.admin.CreateUserRequest;
import com.msagro.security.model.admin.UpdateUserRequest;
import com.msagro.security.model.admin.UserAdminView;
import com.msagro.security.model.auth.AuthenticatedPrincipal;
import com.msagro.security.model.auth.ClientContext;
import com.msagro.security.model.enums.AuditEventType;
import com.msagro.security.model.error.BusinessException;
import com.msagro.security.model.error.ConflictException;
import com.msagro.security.model.error.NotFoundException;
import com.msagro.security.model.person.Person;
import com.msagro.security.model.role.Role;
import com.msagro.security.model.securityuser.SecurityUser;
import com.msagro.security.model.session.SessionView;
import com.msagro.security.model.userapplication.UserApplication;
import com.msagro.security.model.userrole.UserRole;
import com.msagro.security.usecase.business.SessionUseCase;
import com.msagro.security.usecase.business.UserAdministrationUseCase;
import com.msagro.security.usecase.gateway.security.PasswordHasherPort;
import com.msagro.security.usecase.gateway.securitydb.PersonRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.RefreshTokenRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.RoleRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.SecurityUserRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.UserApplicationRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.UserRoleRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Administration of the users of one application (HU-82).
 *
 * <p>The application is always the one of the caller's token, and a user is reached only through
 * their access grant to it: an administrator of one application cannot see or touch the users of
 * another. A user has one role in the application, because permissions are given to roles and
 * never to a person.</p>
 *
 * <p>Nothing is deleted. Deactivating turns the grant off, closes every session and keeps the
 * person, the credentials and the audit trail. Every change that alters what the user may do
 * bumps {@code security_stamp}, so tokens issued before it stop being accepted.</p>
 */
@Service
public class UserAdministrationUseCaseImpl implements UserAdministrationUseCase {

    private final PersonRepositoryPort personRepository;
    private final SecurityUserRepositoryPort userRepository;
    private final UserApplicationRepositoryPort userApplicationRepository;
    private final UserRoleRepositoryPort userRoleRepository;
    private final RoleRepositoryPort roleRepository;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final PasswordHasherPort passwordHasher;
    private final SessionUseCase sessionUseCase;
    private final AuditRecorder auditRecorder;

    public UserAdministrationUseCaseImpl(PersonRepositoryPort personRepository,
                                         SecurityUserRepositoryPort userRepository,
                                         UserApplicationRepositoryPort userApplicationRepository,
                                         UserRoleRepositoryPort userRoleRepository,
                                         RoleRepositoryPort roleRepository,
                                         RefreshTokenRepositoryPort refreshTokenRepository,
                                         PasswordHasherPort passwordHasher,
                                         SessionUseCase sessionUseCase,
                                         AuditRecorder auditRecorder) {
        this.personRepository = personRepository;
        this.userRepository = userRepository;
        this.userApplicationRepository = userApplicationRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
        this.sessionUseCase = sessionUseCase;
        this.auditRecorder = auditRecorder;
    }

    @Override
    public Flux<Role> roles(AuthenticatedPrincipal actor) {
        return roleRepository.findByApplication(actor.getApplicationId())
                .filter(role -> Boolean.TRUE.equals(role.getActive()));
    }

    @Override
    public Flux<UserAdminView> list(AuthenticatedPrincipal actor) {
        return userApplicationRepository.findByApplication(actor.getApplicationId())
                .concatMap(this::view);
    }

    @Override
    @Transactional
    public Mono<UserAdminView> create(CreateUserRequest request, AuthenticatedPrincipal actor,
                                      ClientContext context) {
        Instant now = Instant.now();
        return role(request.getRoleCode(), actor)
                .flatMap(role -> userRepository.existsByUsernameIgnoreCase(request.getUsername())
                        .flatMap(taken -> Boolean.TRUE.equals(taken)
                                ? Mono.<Person>error(new ConflictException("Username already in use"))
                                : resolvePerson(request, actor, now))
                        .flatMap(person -> passwordHasher.hash(request.getTemporaryPassword())
                                .flatMap(hash -> userRepository.save(SecurityUser.builder()
                                        .personId(person.getId())
                                        .username(request.getUsername().trim())
                                        .passwordHash(hash)
                                        .enabled(true)
                                        .locked(false)
                                        .failedAttempts(0)
                                        .credentialsExpired(false)
                                        // A password somebody else chose is changed at the first login.
                                        .mustChangePassword(true)
                                        .passwordChangedAt(now)
                                        .securityStamp(0L)
                                        .createdAt(now)
                                        .createdBy(actor.getSecurityUserId())
                                        .build())))
                        .flatMap(user -> userApplicationRepository.save(UserApplication.builder()
                                        .securityUserId(user.getId())
                                        .applicationId(actor.getApplicationId())
                                        .active(true)
                                        .createdAt(now)
                                        .createdBy(actor.getSecurityUserId())
                                        .build())
                                .flatMap(grant -> userRoleRepository.save(UserRole.builder()
                                                .userApplicationId(grant.getId())
                                                .roleId(role.getId())
                                                .applicationId(grant.getApplicationId())
                                                .active(true)
                                                .createdAt(now)
                                                .createdBy(actor.getSecurityUserId())
                                                .build())
                                        .then(auditRecorder.record(AuditEventType.USER_CREATED,
                                                actor.getSecurityUserId(), actor.getApplicationId(), true,
                                                "Created user " + user.getUsername() + " with role "
                                                        + role.getCode(),
                                                "SECURITY_USER", String.valueOf(user.getId()), null, context))
                                        .then(view(grant)))));
    }

    @Override
    @Transactional
    public Mono<UserAdminView> update(Long securityUserId, UpdateUserRequest request,
                                      AuthenticatedPrincipal actor, ClientContext context) {
        Instant now = Instant.now();
        return grantOf(securityUserId, actor)
                .flatMap(grant -> role(request.getRoleCode(), actor)
                        .flatMap(role -> userRepository.findById(securityUserId)
                                .flatMap(user -> personRepository.findById(user.getPersonId()))
                                .flatMap(person -> ensureEmailFree(request.getEmail(), person.getId())
                                        .then(Mono.defer(() -> {
                                            person.setFirstName(request.getFirstName().trim());
                                            person.setLastName(request.getLastName().trim());
                                            person.setEmail(request.getEmail().trim());
                                            person.setMobile(blankToNull(request.getMobile()));
                                            person.setUpdatedAt(now);
                                            person.setUpdatedBy(actor.getSecurityUserId());
                                            return personRepository.save(person);
                                        })))
                                .then(setOnlyRole(grant, role, actor, now))
                                .then(auditRecorder.record(AuditEventType.USER_UPDATED,
                                        actor.getSecurityUserId(), actor.getApplicationId(), true,
                                        "Updated user " + securityUserId + "; role " + role.getCode(),
                                        "SECURITY_USER", String.valueOf(securityUserId), null, context))
                                .then(view(grant))));
    }

    @Override
    @Transactional
    public Mono<Void> deactivate(Long securityUserId, String reason, AuthenticatedPrincipal actor,
                                 ClientContext context) {
        if (Objects.equals(securityUserId, actor.getSecurityUserId())) {
            return Mono.error(new BusinessException("An administrator cannot deactivate their own user"));
        }
        return grantOf(securityUserId, actor)
                .flatMap(grant -> userApplicationRepository.deactivate(grant.getId())
                        .then(refreshTokenRepository.revokeAllForGrant(grant.getId(), Instant.now()))
                        .then(userRepository.bumpSecurityStamp(securityUserId))
                        .then(auditRecorder.record(AuditEventType.USER_DEACTIVATED,
                                actor.getSecurityUserId(), actor.getApplicationId(), true,
                                "Deactivated user " + securityUserId + ". Reason: " + reason,
                                "USER_APPLICATION", String.valueOf(grant.getId()), null, context)));
    }

    @Override
    @Transactional
    public Mono<Void> activate(Long securityUserId, AuthenticatedPrincipal actor, ClientContext context) {
        return grantOf(securityUserId, actor)
                .flatMap(grant -> userRepository.findById(securityUserId)
                        .filter(user -> Boolean.TRUE.equals(user.getEnabled()))
                        // enabled is the account in every application; one application does not turn it on.
                        .switchIfEmpty(Mono.error(new BusinessException(
                                "The account is disabled for every application; a security administrator must enable it")))
                        .thenReturn(grant))
                .flatMap(grant -> userApplicationRepository.activate(grant.getId())
                        .then(userRepository.bumpSecurityStamp(securityUserId))
                        .then(auditRecorder.record(AuditEventType.USER_ACTIVATED,
                                actor.getSecurityUserId(), actor.getApplicationId(), true,
                                "Activated user " + securityUserId,
                                "USER_APPLICATION", String.valueOf(grant.getId()), null, context)));
    }

    /** The password belongs to the person, so the sessions of every application are closed. */
    @Override
    @Transactional
    public Mono<Void> resetPassword(Long securityUserId, String temporaryPassword,
                                    AuthenticatedPrincipal actor, ClientContext context) {
        Instant now = Instant.now();
        return grantOf(securityUserId, actor)
                .flatMap(grant -> passwordHasher.hash(temporaryPassword)
                        .flatMap(hash -> userRepository.setTemporaryPassword(securityUserId, hash, now))
                        .thenMany(userApplicationRepository.findByUser(securityUserId))
                        .concatMap(any -> refreshTokenRepository.revokeAllForGrant(any.getId(), now))
                        .then(auditRecorder.record(AuditEventType.PASSWORD_RESET_BY_ADMIN,
                                actor.getSecurityUserId(), actor.getApplicationId(), true,
                                "Temporary password set for user " + securityUserId,
                                "SECURITY_USER", String.valueOf(securityUserId), null, context)));
    }

    @Override
    public Flux<SessionView> sessions(Long securityUserId, AuthenticatedPrincipal actor) {
        return grantOf(securityUserId, actor)
                .flatMapMany(grant -> sessionUseCase.listSessions(grant.getId()));
    }

    @Override
    public Mono<Void> revokeSession(Long securityUserId, Long sessionId, String reason,
                                    AuthenticatedPrincipal actor, ClientContext context) {
        return grantOf(securityUserId, actor)
                .flatMap(grant -> sessionUseCase.revokeSession(grant.getId(), sessionId, reason, actor, context));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** The user is reached only through a grant to the caller's application. */
    private Mono<UserApplication> grantOf(Long securityUserId, AuthenticatedPrincipal actor) {
        return userApplicationRepository.findByUserAndApplication(securityUserId, actor.getApplicationId())
                .switchIfEmpty(Mono.error(new NotFoundException("User", securityUserId)));
    }

    private Mono<Role> role(String code, AuthenticatedPrincipal actor) {
        return roleRepository.findByApplicationAndCode(actor.getApplicationId(), code == null ? "" : code.trim())
                .filter(role -> Boolean.TRUE.equals(role.getActive()))
                .switchIfEmpty(Mono.error(new BusinessException(
                        "Role " + code + " does not exist in this application")));
    }

    /**
     * Reuses a person with that identification when they have no credentials yet; a person who
     * already has a user is a conflict, not a take-over.
     */
    private Mono<Person> resolvePerson(CreateUserRequest request, AuthenticatedPrincipal actor, Instant now) {
        return personRepository.findByIdentification(request.getIdentificationType(),
                        request.getIdentificationNumber())
                .flatMap(existing -> userRepository.findByPersonId(existing.getId())
                        .flatMap(user -> Mono.<Person>error(new ConflictException(
                                "This person already has a user: " + user.getUsername())))
                        .switchIfEmpty(Mono.just(existing)))
                .switchIfEmpty(Mono.defer(() -> ensureEmailFree(request.getEmail(), null)
                        .then(personRepository.save(Person.builder()
                                .identificationType(request.getIdentificationType().trim())
                                .identificationNumber(request.getIdentificationNumber().trim())
                                .firstName(request.getFirstName().trim())
                                .lastName(request.getLastName().trim())
                                .email(request.getEmail().trim())
                                .mobile(blankToNull(request.getMobile()))
                                .active(true)
                                .createdAt(now)
                                .createdBy(actor.getSecurityUserId())
                                .build()))));
    }

    private Mono<Void> ensureEmailFree(String email, Long ownerPersonId) {
        return personRepository.findByEmailIgnoreCase(email == null ? "" : email.trim())
                .filter(other -> !Objects.equals(other.getId(), ownerPersonId))
                .flatMap(other -> Mono.<Void>error(new ConflictException("Email already in use")));
    }

    /** Leaves the grant with exactly one active role; the stamp moves only when that changes. */
    private Mono<Void> setOnlyRole(UserApplication grant, Role role, AuthenticatedPrincipal actor, Instant now) {
        return userRoleRepository.findByGrant(grant.getId()).collectList()
                .flatMap(assignments -> {
                    List<UserRole> others = assignments.stream()
                            .filter(a -> Boolean.TRUE.equals(a.getActive()) && !a.getRoleId().equals(role.getId()))
                            .toList();
                    UserRole current = assignments.stream()
                            .filter(a -> a.getRoleId().equals(role.getId()))
                            .findFirst().orElse(null);
                    boolean alreadyOn = current != null && Boolean.TRUE.equals(current.getActive())
                            && current.getValidUntil() == null;
                    if (others.isEmpty() && alreadyOn) {
                        return Mono.<Void>empty();
                    }
                    Mono<Void> turnOn = current == null
                            ? userRoleRepository.save(UserRole.builder()
                                    .userApplicationId(grant.getId())
                                    .roleId(role.getId())
                                    .applicationId(grant.getApplicationId())
                                    .active(true)
                                    .createdAt(now)
                                    .createdBy(actor.getSecurityUserId())
                                    .build()).then()
                            : userRoleRepository.activate(grant.getId(), role.getId());
                    return Flux.fromIterable(others)
                            .concatMap(a -> userRoleRepository.deactivate(grant.getId(), a.getRoleId()))
                            .then(turnOn)
                            .then(userRepository.bumpSecurityStamp(grant.getSecurityUserId()));
                });
    }

    private Mono<UserAdminView> view(UserApplication grant) {
        Instant now = Instant.now();
        return userRepository.findById(grant.getSecurityUserId())
                .flatMap(user -> Mono.zip(
                                personRepository.findById(user.getPersonId()),
                                userRoleRepository.findRoleCodes(grant.getId()).collectList(),
                                refreshTokenRepository.findOpenSessions(grant.getId(), now).count())
                        .map(t -> {
                            Person person = t.getT1();
                            return UserAdminView.builder()
                                    .securityUserId(user.getId())
                                    .userApplicationId(grant.getId())
                                    .username(user.getUsername())
                                    .firstName(person.getFirstName())
                                    .lastName(person.getLastName())
                                    .fullName(person.fullName())
                                    .email(person.getEmail())
                                    .identificationType(person.getIdentificationType())
                                    .identificationNumber(person.getIdentificationNumber())
                                    .mobile(person.getMobile())
                                    .roles(t.getT2())
                                    .active(Boolean.TRUE.equals(user.getEnabled()) && grant.isUsableAt(now))
                                    .mustChangePassword(Boolean.TRUE.equals(user.getMustChangePassword()))
                                    .lastLoginAt(user.getLastLoginAt())
                                    .openSessions(t.getT3().intValue())
                                    .build();
                        }));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
