package com.msagro.security.usecase.gateway.securitydb;

import com.msagro.security.model.profession.Profession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Output port for {@code profession}. */
public interface ProfessionRepositoryPort {

    Mono<Profession> save(Profession profession);

    Mono<Profession> findById(Long id);

    Mono<Profession> findByCode(String code);

    Flux<Profession> findAllActive();
}
