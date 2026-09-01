package com.msagro.security.usecase;

import com.msagro.security.model.auth.AuthResponse;
import com.msagro.security.model.auth.ClientContext;
import com.msagro.security.model.auth.RegisterRequest;
import com.msagro.security.model.enums.AuditEventType;
import com.msagro.security.model.error.ConflictException;
import com.msagro.security.model.error.NotFoundException;
import com.msagro.security.model.person.Person;
import com.msagro.security.model.securityuser.SecurityUser;
import com.msagro.security.model.userapplication.UserApplication;
import com.msagro.security.model.userrole.UserRole;
import com.msagro.security.usecase.business.RegisterUserUseCase;
import com.msagro.security.usecase.config.SecurityPolicyProperties;
import com.msagro.security.usecase.gateway.security.PasswordHasherPort;
import com.msagro.security.usecase.gateway.securitydb.ApplicationRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.PersonRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.RoleRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.SecurityUserRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.UserApplicationRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.UserRoleRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Registration use case.
 *
 * <p>Registration walks the three layers of the model in order: the {@code person} (created, or
 * reused when the identification already exists), the {@code security_user} credentials, and the
 * {@code user_application} grant that actually lets the account in. The default role of the
 * target application is assigned when it exists.</p>
 *
 * <p>A person who already has credentials is not silently taken over: registering again with the
 * same identification is a conflict, not a merge.</p>
 */
@Service
public class RegisterUserUseCaseImpl implements RegisterUserUseCase {

    private final ApplicationRepositoryPort applicationRepository;
    private final PersonRepositoryPort personRepository;
    private final SecurityUserRepositoryPort userRepository;
    private final UserApplicationRepositoryPort userApplicationRepository;
    private final UserRoleRepositoryPort userRoleRepository;
    private final RoleRepositoryPort roleRepository;
    private final PasswordHasherPort passwordHasher;
    private final SecurityPolicyProperties policy;
    private final TokenIssuer tokenIssuer;
    private final AuditRecorder auditRecorder;

    public RegisterUserUseCaseImpl(ApplicationRepositoryPort applicationRepository,
                                   PersonRepositoryPort personRepository,
                                   SecurityUserRepositoryPort userRepository,
                                   UserApplicationRepositoryPort userApplicationRepository,
                                   UserRoleRepositoryPort userRoleRepository,
                                   RoleRepositoryPort roleRepository,
                                   PasswordHasherPort passwordHasher,
                                   SecurityPolicyProperties policy,
                                   TokenIssuer tokenIssuer,
                                   AuditRecorder auditRecorder) {
        this.applicationRepository = applicationRepository;
        this.personRepository = personRepository;
        this.userRepository = userRepository;
        this.userApplicationRepository = userApplicationRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.passwordHasher = passwordHasher;
        this.policy = policy;
        this.tokenIssuer = tokenIssuer;
        this.auditRecorder = auditRecorder;
    }

    @Override
    @Transactional
    public Mono<AuthResponse> register(RegisterRequest request, ClientContext context) {
        return applicationRepository.findById(request.getApplicationId())
                .switchIfEmpty(Mono.error(new NotFoundException("Application", request.getApplicationId())))
                .filter(application -> Boolean.TRUE.equals(application.getActive()))
                .switchIfEmpty(Mono.error(new ConflictException("Application is not active")))
                .then(ensureUsernameFree(request))
                .then(resolvePerson(request))
                .flatMap(person -> createCredentials(request, person))
                .flatMap(user -> grantAccess(request, user)
                        .flatMap(grant -> assignDefaultRole(request.getApplicationId(), grant)
                                .then(auditRecorder.record(AuditEventType.USER_REGISTERED, user.getId(),
                                        request.getApplicationId(), true,
                                        "User " + user.getUsername() + " registered", context))
                                .then(tokenIssuer.issueNewSession(user, grant, context))));
    }

    // ── steps ────────────────────────────────────────────────────────────────

    private Mono<Void> ensureUsernameFree(RegisterRequest request) {
        return userRepository.existsByUsernameIgnoreCase(request.getUsername())
                .flatMap(taken -> Boolean.TRUE.equals(taken)
                        ? Mono.error(new ConflictException("Username already in use"))
                        : Mono.empty());
    }

    /**
     * Reuses the person when the identification already exists — but only if that person has no
     * credentials yet. The e-mail is checked separately because it carries its own unique index.
     */
    private Mono<Person> resolvePerson(RegisterRequest request) {
        return personRepository
                .findByIdentification(request.getIdentificationType(), request.getIdentificationNumber())
                .flatMap(existing -> userRepository.findByPersonId(existing.getId())
                        .flatMap(user -> Mono.<Person>error(new ConflictException(
                                "This person already has a user account")))
                        .switchIfEmpty(Mono.just(existing)))
                .switchIfEmpty(Mono.defer(() -> createPerson(request)));
    }

    private Mono<Person> createPerson(RegisterRequest request) {
        return personRepository.findByEmailIgnoreCase(request.getEmail())
                .flatMap(taken -> Mono.<Person>error(new ConflictException("Email already in use")))
                .switchIfEmpty(Mono.defer(() -> personRepository.save(Person.builder()
                        .professionId(request.getProfessionId())
                        .identificationType(request.getIdentificationType())
                        .identificationNumber(request.getIdentificationNumber())
                        .firstName(request.getFirstName())
                        .middleName(request.getMiddleName())
                        .lastName(request.getLastName())
                        .secondLastName(request.getSecondLastName())
                        .email(request.getEmail())
                        .phone(request.getPhone())
                        .mobile(request.getMobile())
                        .birthDate(request.getBirthDate())
                        .active(true)
                        .createdAt(Instant.now())
                        .build())));
    }

    private Mono<SecurityUser> createCredentials(RegisterRequest request, Person person) {
        Instant now = Instant.now();
        return passwordHasher.hash(request.getPassword())
                .flatMap(passwordHash -> userRepository.save(SecurityUser.builder()
                        .personId(person.getId())
                        .username(request.getUsername())
                        .passwordHash(passwordHash)
                        .enabled(true)
                        .locked(false)
                        .failedAttempts(0)
                        .credentialsExpired(false)
                        .mustChangePassword(policy.isRequirePasswordChangeOnRegister())
                        .passwordChangedAt(now)
                        .securityStamp(0L)
                        .createdAt(now)
                        .build()));
    }

    private Mono<UserApplication> grantAccess(RegisterRequest request, SecurityUser user) {
        return userApplicationRepository.save(UserApplication.builder()
                .securityUserId(user.getId())
                .applicationId(request.getApplicationId())
                .active(true)
                .createdAt(Instant.now())
                .build());
    }

    /** Silently skips when the application has no role with the configured default code. */
    private Mono<Void> assignDefaultRole(Long applicationId, UserApplication grant) {
        return roleRepository.findByApplicationAndCode(applicationId, policy.getDefaultRoleCode())
                .filter(role -> Boolean.TRUE.equals(role.getActive()))
                .flatMap(role -> userRoleRepository.save(UserRole.builder()
                        .userApplicationId(grant.getId())
                        .roleId(role.getId())
                        .applicationId(applicationId)
                        .active(true)
                        .createdAt(Instant.now())
                        .build()))
                .then();
    }
}
