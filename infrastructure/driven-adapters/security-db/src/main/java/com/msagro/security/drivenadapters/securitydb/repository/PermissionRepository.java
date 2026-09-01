package com.msagro.security.drivenadapters.securitydb.repository;

import com.msagro.security.drivenadapters.securitydb.entity.PermissionEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface PermissionRepository extends ReactiveCrudRepository<PermissionEntity, Long> {

    Mono<PermissionEntity> findByApplicationIdAndCode(Long applicationId, String code);

    Flux<PermissionEntity> findByApplicationIdOrderByCodeAsc(Long applicationId);
}
