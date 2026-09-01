package com.msagro.security.drivenadapters.securitydb.repository;

import com.msagro.security.drivenadapters.securitydb.entity.ApplicationEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ApplicationRepository extends ReactiveCrudRepository<ApplicationEntity, Long> {

    Mono<ApplicationEntity> findByCompanyIdAndCode(Long companyId, String code);

    Flux<ApplicationEntity> findByCompanyIdOrderByCodeAsc(Long companyId);
}
