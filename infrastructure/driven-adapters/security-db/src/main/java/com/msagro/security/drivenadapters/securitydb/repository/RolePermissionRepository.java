package com.msagro.security.drivenadapters.securitydb.repository;

import com.msagro.security.drivenadapters.securitydb.entity.RolePermissionEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface RolePermissionRepository extends ReactiveCrudRepository<RolePermissionEntity, Long> {

    Mono<RolePermissionEntity> findByRoleIdAndPermissionId(Long roleId, Long permissionId);
}
