package com.msagro.security.usecase.gateway.securitydb;

import com.msagro.security.model.application.Application;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Output port for {@code application}. */
public interface ApplicationRepositoryPort {

    Mono<Application> save(Application application);

    Mono<Application> findById(Long id);

    Mono<Application> findByCompanyAndCode(Long companyId, String code);

    Flux<Application> findByCompany(Long companyId);
}
