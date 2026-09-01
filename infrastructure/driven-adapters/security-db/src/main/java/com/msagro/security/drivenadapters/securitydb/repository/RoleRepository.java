package com.msagro.security.drivenadapters.securitydb.repository;

import com.msagro.security.drivenadapters.securitydb.entity.RoleEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface RoleRepository extends ReactiveCrudRepository<RoleEntity, Long> {

    Mono<RoleEntity> findByApplicationIdAndCode(Long applicationId, String code);

    Flux<RoleEntity> findByApplicationIdOrderByCodeAsc(Long applicationId);
}
