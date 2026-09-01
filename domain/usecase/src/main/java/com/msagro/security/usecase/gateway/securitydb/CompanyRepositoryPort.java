package com.msagro.security.usecase.gateway.securitydb;

import com.msagro.security.model.company.Company;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Output port for {@code company}. */
public interface CompanyRepositoryPort {

    Mono<Company> save(Company company);

    Mono<Company> findById(Long id);

    Mono<Company> findByCode(String code);

    Flux<Company> findAllActive();
}
