package com.msagro.security.drivenadapters.securitydb.adapter;

import com.msagro.security.drivenadapters.securitydb.mapper.PermissionMapper;
import com.msagro.security.drivenadapters.securitydb.repository.PermissionRepository;
import com.msagro.security.model.permission.Permission;
import com.msagro.security.usecase.gateway.securitydb.PermissionRepositoryPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** R2DBC implementation of the permission output port. */
@Component
public class PermissionAdapter implements PermissionRepositoryPort {

    private final PermissionRepository repository;
    private final PermissionMapper mapper;

    public PermissionAdapter(PermissionRepository repository, PermissionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Permission> save(Permission permission) {
        return repository.save(mapper.toEntity(permission)).map(mapper::toModel);
    }

    @Override
    public Mono<Permission> findById(Long id) {
        return repository.findById(id).map(mapper::toModel);
    }

    @Override
    public Mono<Permission> findByApplicationAndCode(Long applicationId, String code) {
        return repository.findByApplicationIdAndCode(applicationId, code).map(mapper::toModel);
    }

    @Override
    public Flux<Permission> findByApplication(Long applicationId) {
        return repository.findByApplicationIdOrderByCodeAsc(applicationId).map(mapper::toModel);
    }
}
