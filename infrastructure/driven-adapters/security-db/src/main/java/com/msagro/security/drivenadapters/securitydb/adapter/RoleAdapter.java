package com.msagro.security.drivenadapters.securitydb.adapter;

import com.msagro.security.drivenadapters.securitydb.mapper.RoleMapper;
import com.msagro.security.drivenadapters.securitydb.repository.RoleRepository;
import com.msagro.security.model.role.Role;
import com.msagro.security.usecase.gateway.securitydb.RoleRepositoryPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** R2DBC implementation of the role output port. */
@Component
public class RoleAdapter implements RoleRepositoryPort {

    private final RoleRepository repository;
    private final RoleMapper mapper;

    public RoleAdapter(RoleRepository repository, RoleMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Role> save(Role role) {
        return repository.save(mapper.toEntity(role)).map(mapper::toModel);
    }

    @Override
    public Mono<Role> findById(Long id) {
        return repository.findById(id).map(mapper::toModel);
    }

    @Override
    public Mono<Role> findByApplicationAndCode(Long applicationId, String code) {
        return repository.findByApplicationIdAndCode(applicationId, code).map(mapper::toModel);
    }

    @Override
    public Flux<Role> findByApplication(Long applicationId) {
        return repository.findByApplicationIdOrderByCodeAsc(applicationId).map(mapper::toModel);
    }
}
