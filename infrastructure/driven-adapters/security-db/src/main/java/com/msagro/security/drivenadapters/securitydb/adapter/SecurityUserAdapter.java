package com.msagro.security.drivenadapters.securitydb.adapter;

import com.msagro.security.drivenadapters.securitydb.mapper.DateTimeMapper;
import com.msagro.security.drivenadapters.securitydb.mapper.SecurityUserMapper;
import com.msagro.security.drivenadapters.securitydb.repository.SecurityUserRepository;
import com.msagro.security.model.securityuser.SecurityUser;
import com.msagro.security.usecase.gateway.securitydb.SecurityUserRepositoryPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * R2DBC implementation of the user output port.
 *
 * <p>The state transitions (failed attempt, lock, password change) are single UPDATE statements
 * rather than read-modify-write cycles, so two concurrent logins cannot lose an increment of
 * {@code failed_attempts} or of {@code security_stamp}.</p>
 */
@Component
public class SecurityUserAdapter implements SecurityUserRepositoryPort {

    private final SecurityUserRepository repository;
    private final SecurityUserMapper mapper;
    private final DateTimeMapper dateTimeMapper;

    public SecurityUserAdapter(SecurityUserRepository repository,
                               SecurityUserMapper mapper,
                               DateTimeMapper dateTimeMapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.dateTimeMapper = dateTimeMapper;
    }

    @Override
    public Mono<SecurityUser> save(SecurityUser user) {
        return repository.save(mapper.toEntity(user)).map(mapper::toModel);
    }

    @Override
    public Mono<SecurityUser> findById(Long id) {
        return repository.findById(id).map(mapper::toModel);
    }

    @Override
    public Mono<SecurityUser> findByUsernameIgnoreCase(String username) {
        return repository.findByUsernameIgnoreCase(username).map(mapper::toModel);
    }

    @Override
    public Mono<SecurityUser> findByPersonId(Long personId) {
        return repository.findByPersonId(personId).map(mapper::toModel);
    }

    @Override
    public Mono<Boolean> existsByUsernameIgnoreCase(String username) {
        return repository.existsByUsernameIgnoreCase(username);
    }

    @Override
    public Mono<Void> registerSuccessfulLogin(Long userId, Instant when) {
        return repository.registerSuccessfulLogin(userId, dateTimeMapper.toOffsetDateTime(when)).then();
    }

    @Override
    public Mono<Integer> registerFailedAttempt(Long userId) {
        return repository.registerFailedAttempt(userId).map(Short::intValue);
    }

    @Override
    public Mono<Void> lockUntil(Long userId, Instant until) {
        return repository.lockUntil(userId, dateTimeMapper.toOffsetDateTime(until)).then();
    }

    @Override
    public Mono<Void> updatePassword(Long userId, String passwordHash, Instant when) {
        return repository.updatePassword(userId, passwordHash, dateTimeMapper.toOffsetDateTime(when)).then();
    }

    @Override
    public Mono<Void> bumpSecurityStamp(Long userId) {
        return repository.bumpSecurityStamp(userId).then();
    }
}
