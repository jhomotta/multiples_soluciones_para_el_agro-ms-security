package com.msagro.security.usecase.gateway.securitydb;

import com.msagro.security.model.role.Role;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Output port for {@code role}. */
public interface RoleRepositoryPort {

    Mono<Role> save(Role role);

    Mono<Role> findById(Long id);

    Mono<Role> findByApplicationAndCode(Long applicationId, String code);

    Flux<Role> findByApplication(Long applicationId);
}
