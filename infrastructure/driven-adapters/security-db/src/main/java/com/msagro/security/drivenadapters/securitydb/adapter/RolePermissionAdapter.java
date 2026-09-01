package com.msagro.security.drivenadapters.securitydb.adapter;

import com.msagro.security.drivenadapters.securitydb.mapper.RolePermissionMapper;
import com.msagro.security.drivenadapters.securitydb.repository.RolePermissionRepository;
import com.msagro.security.model.rolepermission.RolePermission;
import com.msagro.security.usecase.gateway.securitydb.RolePermissionRepositoryPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** R2DBC implementation of the role-permission output port. */
@Component
public class RolePermissionAdapter implements RolePermissionRepositoryPort {

    private final RolePermissionRepository repository;
    private final RolePermissionMapper mapper;

    public RolePermissionAdapter(RolePermissionRepository repository, RolePermissionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<RolePermission> save(RolePermission rolePermission) {
        return repository.save(mapper.toEntity(rolePermission)).map(mapper::toModel);
    }

    @Override
    public Mono<RolePermission> findByRoleAndPermission(Long roleId, Long permissionId) {
        return repository.findByRoleIdAndPermissionId(roleId, permissionId).map(mapper::toModel);
    }
}
