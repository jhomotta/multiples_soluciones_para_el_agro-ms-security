package com.msagro.security.drivenadapters.securitydb.adapter;

import com.msagro.security.drivenadapters.securitydb.mapper.UserRoleMapper;
import com.msagro.security.drivenadapters.securitydb.repository.UserRoleRepository;
import com.msagro.security.model.userrole.UserRole;
import com.msagro.security.usecase.gateway.securitydb.UserRoleRepositoryPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** R2DBC implementation of the role-assignment output port. */
@Component
public class UserRoleAdapter implements UserRoleRepositoryPort {

    private final UserRoleRepository repository;
    private final UserRoleMapper mapper;

    public UserRoleAdapter(UserRoleRepository repository, UserRoleMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<UserRole> save(UserRole userRole) {
        return repository.save(mapper.toEntity(userRole)).map(mapper::toModel);
    }

    @Override
    public Mono<UserRole> findByGrantAndRole(Long userApplicationId, Long roleId) {
        return repository.findByUserApplicationIdAndRoleId(userApplicationId, roleId).map(mapper::toModel);
    }

    @Override
    public Flux<UserRole> findByGrant(Long userApplicationId) {
        return repository.findByUserApplicationIdOrderByRoleIdAsc(userApplicationId).map(mapper::toModel);
    }

    @Override
    public Mono<Void> deactivate(Long userApplicationId, Long roleId) {
        return repository.deactivate(userApplicationId, roleId).then();
    }

    @Override
    public Flux<String> findAuthorities(Long userApplicationId) {
        return repository.findAuthorities(userApplicationId);
    }

    @Override
    public Flux<String> findRoleCodes(Long userApplicationId) {
        return repository.findRoleCodes(userApplicationId);
    }
}
