package com.msagro.security.configuration;

import com.msagro.security.model.person.Person;
import com.msagro.security.model.securityuser.SecurityUser;
import com.msagro.security.model.userapplication.UserApplication;
import com.msagro.security.model.userrole.UserRole;
import com.msagro.security.usecase.gateway.security.PasswordHasherPort;
import com.msagro.security.usecase.gateway.securitydb.ApplicationRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.CompanyRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.PersonRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.RoleRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.SecurityUserRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.UserApplicationRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.UserRoleRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Creates the first administrator on start-up, once, when {@code security.bootstrap.enabled} is on.
 *
 * <p>It is a no-op as soon as the username exists, so restarting the service never resets or
 * overwrites an account. Turn it off again once the administrator has been created and its
 * password changed — leaving a bootstrap password in the environment is exactly the kind of
 * long-lived credential this service exists to avoid.</p>
 */
@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final BootstrapProperties properties;
    private final CompanyRepositoryPort companyRepository;
    private final ApplicationRepositoryPort applicationRepository;
    private final PersonRepositoryPort personRepository;
    private final SecurityUserRepositoryPort userRepository;
    private final UserApplicationRepositoryPort userApplicationRepository;
    private final UserRoleRepositoryPort userRoleRepository;
    private final RoleRepositoryPort roleRepository;
    private final PasswordHasherPort passwordHasher;

    public BootstrapAdminRunner(BootstrapProperties properties,
                                CompanyRepositoryPort companyRepository,
                                ApplicationRepositoryPort applicationRepository,
                                PersonRepositoryPort personRepository,
                                SecurityUserRepositoryPort userRepository,
                                UserApplicationRepositoryPort userApplicationRepository,
                                UserRoleRepositoryPort userRoleRepository,
                                RoleRepositoryPort roleRepository,
                                PasswordHasherPort passwordHasher) {
        this.properties = properties;
        this.companyRepository = companyRepository;
        this.applicationRepository = applicationRepository;
        this.personRepository = personRepository;
        this.userRepository = userRepository;
        this.userApplicationRepository = userApplicationRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        if (properties.getPassword() == null || properties.getPassword().isBlank()) {
            log.warn("security.bootstrap.enabled is true but no password was provided; "
                    + "set BOOTSTRAP_ADMIN_PASSWORD. Skipping.");
            return;
        }

        // Start-up runs once, outside the event loop, so blocking here is safe and keeps the
        // service from accepting traffic before the administrator exists.
        Boolean created = createIfMissing().block();
        if (Boolean.TRUE.equals(created)) {
            log.info("Bootstrap administrator '{}' created for application '{}'",
                    properties.getUsername(), properties.getApplicationCode());
        }
    }

    private Mono<Boolean> createIfMissing() {
        return userRepository.existsByUsernameIgnoreCase(properties.getUsername())
                .flatMap(exists -> Boolean.TRUE.equals(exists)
                        ? Mono.just(false)
                        : resolveApplication().flatMap(this::createAdmin))
                .onErrorResume(error -> {
                    log.error("Could not create the bootstrap administrator", error);
                    return Mono.just(false);
                });
    }

    /** Finds the target application by code across the seeded companies. */
    private Mono<com.msagro.security.model.application.Application> resolveApplication() {
        return companyRepository.findAllActive()
                .concatMap(company -> applicationRepository
                        .findByCompanyAndCode(company.getId(), properties.getApplicationCode()))
                .next()
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "No application with code " + properties.getApplicationCode()
                                + " exists; run the seed migration first")));
    }

    private Mono<Boolean> createAdmin(com.msagro.security.model.application.Application application) {
        Instant now = Instant.now();

        return personRepository.save(Person.builder()
                        .identificationType(properties.getIdentificationType())
                        .identificationNumber(properties.getIdentificationNumber())
                        .firstName(properties.getFirstName())
                        .lastName(properties.getLastName())
                        .email(properties.getEmail())
                        .active(true)
                        .createdAt(now)
                        .build())
                .flatMap(person -> passwordHasher.hash(properties.getPassword())
                        .flatMap(hash -> userRepository.save(SecurityUser.builder()
                                .personId(person.getId())
                                .username(properties.getUsername())
                                .passwordHash(hash)
                                .enabled(true)
                                .locked(false)
                                .failedAttempts(0)
                                .credentialsExpired(false)
                                .mustChangePassword(properties.isMustChangePassword())
                                .passwordChangedAt(now)
                                .securityStamp(0L)
                                .createdAt(now)
                                .build())))
                .flatMap(user -> userApplicationRepository.save(UserApplication.builder()
                        .securityUserId(user.getId())
                        .applicationId(application.getId())
                        .active(true)
                        .createdAt(now)
                        .build()))
                .flatMap(grant -> roleRepository
                        .findByApplicationAndCode(application.getId(), properties.getRoleCode())
                        .switchIfEmpty(Mono.error(new IllegalStateException(
                                "No role " + properties.getRoleCode() + " in application "
                                        + properties.getApplicationCode())))
                        .flatMap(role -> userRoleRepository.save(UserRole.builder()
                                .userApplicationId(grant.getId())
                                .roleId(role.getId())
                                .applicationId(application.getId())
                                .active(true)
                                .createdAt(now)
                                .build())))
                .thenReturn(true);
    }
}
