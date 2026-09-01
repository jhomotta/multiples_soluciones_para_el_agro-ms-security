package com.msagro.security.drivenadapters.securitydb.repository;

import com.msagro.security.drivenadapters.securitydb.entity.UserRoleEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserRoleRepository extends ReactiveCrudRepository<UserRoleEntity, Long> {

    Mono<UserRoleEntity> findByUserApplicationIdAndRoleId(Long userApplicationId, Long roleId);

    Flux<UserRoleEntity> findByUserApplicationIdOrderByRoleIdAsc(Long userApplicationId);

    @Modifying
    @Query("""
            UPDATE user_role SET active = FALSE
             WHERE user_application_id = :userApplicationId AND role_id = :roleId
            """)
    Mono<Integer> deactivate(@Param("userApplicationId") Long userApplicationId,
                             @Param("roleId") Long roleId);

    /**
     * Authorities of one access grant: the code of every active permission reachable through an
     * active, in-window role, plus {@code ROLE_<code>} for each of those roles. This single query
     * is what the JWT authority list is built from.
     */
    @Query("""
            SELECT p.code AS authority
              FROM user_role ur
              JOIN role r ON r.id = ur.role_id AND r.active
              JOIN role_permission rp ON rp.role_id = r.id
              JOIN permission p ON p.id = rp.permission_id AND p.active
             WHERE ur.user_application_id = :userApplicationId
               AND ur.active
               AND (ur.valid_from IS NULL OR ur.valid_from <= now())
               AND (ur.valid_until IS NULL OR ur.valid_until > now())
            UNION
            SELECT 'ROLE_' || r.code AS authority
              FROM user_role ur
              JOIN role r ON r.id = ur.role_id AND r.active
             WHERE ur.user_application_id = :userApplicationId
               AND ur.active
               AND (ur.valid_from IS NULL OR ur.valid_from <= now())
               AND (ur.valid_until IS NULL OR ur.valid_until > now())
             ORDER BY authority
            """)
    Flux<String> findAuthorities(@Param("userApplicationId") Long userApplicationId);

    @Query("""
            SELECT r.code
              FROM user_role ur
              JOIN role r ON r.id = ur.role_id AND r.active
             WHERE ur.user_application_id = :userApplicationId
               AND ur.active
               AND (ur.valid_from IS NULL OR ur.valid_from <= now())
               AND (ur.valid_until IS NULL OR ur.valid_until > now())
             ORDER BY r.code
            """)
    Flux<String> findRoleCodes(@Param("userApplicationId") Long userApplicationId);
}
