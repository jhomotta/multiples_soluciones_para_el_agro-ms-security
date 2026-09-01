package com.msagro.security.usecase.gateway.securitydb;

import com.msagro.security.model.rolepermission.RolePermission;
import reactor.core.publisher.Mono;

/** Output port for {@code role_permission}. */
public interface RolePermissionRepositoryPort {

    Mono<RolePermission> save(RolePermission rolePermission);

    Mono<RolePermission> findByRoleAndPermission(Long roleId, Long permissionId);
}
