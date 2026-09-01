package com.msagro.security.drivenadapters.securitydb.repository;

import com.msagro.security.drivenadapters.securitydb.entity.CompanyEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CompanyRepository extends ReactiveCrudRepository<CompanyEntity, Long> {

    Mono<CompanyEntity> findByCode(String code);

    Flux<CompanyEntity> findByActiveTrueOrderByCodeAsc();
}
