package com.msagro.security.usecase.gateway.securitydb;

import com.msagro.security.model.permission.Permission;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Output port for {@code permission}. */
public interface PermissionRepositoryPort {

    Mono<Permission> save(Permission permission);

    Mono<Permission> findById(Long id);

    Mono<Permission> findByApplicationAndCode(Long applicationId, String code);

    Flux<Permission> findByApplication(Long applicationId);
}
