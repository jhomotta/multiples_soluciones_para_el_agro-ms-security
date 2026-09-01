package com.msagro.security.usecase.gateway.securitydb;

import com.msagro.security.model.person.Person;
import reactor.core.publisher.Mono;

/** Output port for {@code person}. */
public interface PersonRepositoryPort {

    Mono<Person> save(Person person);

    Mono<Person> findById(Long id);

    Mono<Person> findByIdentification(String identificationType, String identificationNumber);

    /** Case-insensitive lookup, matching the {@code LOWER(email)} unique index. */
    Mono<Person> findByEmailIgnoreCase(String email);
}
