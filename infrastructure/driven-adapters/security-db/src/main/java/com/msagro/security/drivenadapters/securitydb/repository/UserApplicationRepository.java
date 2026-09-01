package com.msagro.security.drivenadapters.securitydb.repository;

import com.msagro.security.drivenadapters.securitydb.entity.UserApplicationEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserApplicationRepository extends ReactiveCrudRepository<UserApplicationEntity, Long> {

    Mono<UserApplicationEntity> findBySecurityUserIdAndApplicationId(Long securityUserId, Long applicationId);

    Flux<UserApplicationEntity> findBySecurityUserIdOrderByApplicationIdAsc(Long securityUserId);

    /** Grants are deactivated, never deleted: the audit trail must keep pointing at them. */
    @Modifying
    @Query("UPDATE user_application SET active = FALSE, updated_at = now() WHERE id = :id")
    Mono<Integer> deactivate(@Param("id") Long id);
}
