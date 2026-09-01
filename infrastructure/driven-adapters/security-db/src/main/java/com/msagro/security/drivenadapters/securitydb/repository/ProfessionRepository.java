package com.msagro.security.drivenadapters.securitydb.repository;

import com.msagro.security.drivenadapters.securitydb.entity.ProfessionEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ProfessionRepository extends ReactiveCrudRepository<ProfessionEntity, Long> {

    Mono<ProfessionEntity> findByCode(String code);

    Flux<ProfessionEntity> findByActiveTrueOrderByNameAsc();
}
