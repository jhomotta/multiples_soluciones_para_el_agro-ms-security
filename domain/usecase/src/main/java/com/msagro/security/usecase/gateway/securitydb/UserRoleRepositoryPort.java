package com.msagro.security.usecase.gateway.securitydb;

import com.msagro.security.model.userrole.UserRole;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Output port for {@code user_role} assignments. */
public interface UserRoleRepositoryPort {

    Mono<UserRole> save(UserRole userRole);

    Mono<UserRole> findByGrantAndRole(Long userApplicationId, Long roleId);

    Flux<UserRole> findByGrant(Long userApplicationId);

    /** Deactivates one assignment; the row stays for audit purposes. */
    Mono<Void> deactivate(Long userApplicationId, Long roleId);

    /**
     * Authority strings of an access grant: every active permission code, plus
     * {@code ROLE_<code>} for every active, in-window role.
     */
    Flux<String> findAuthorities(Long userApplicationId);

    /** Codes of the active, in-window roles of an access grant. */
    Flux<String> findRoleCodes(Long userApplicationId);
}
