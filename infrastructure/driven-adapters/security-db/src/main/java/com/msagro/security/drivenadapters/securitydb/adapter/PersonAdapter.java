package com.msagro.security.drivenadapters.securitydb.adapter;

import com.msagro.security.drivenadapters.securitydb.mapper.PersonMapper;
import com.msagro.security.drivenadapters.securitydb.repository.PersonRepository;
import com.msagro.security.model.person.Person;
import com.msagro.security.usecase.gateway.securitydb.PersonRepositoryPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** R2DBC implementation of the person output port. */
@Component
public class PersonAdapter implements PersonRepositoryPort {

    private final PersonRepository repository;
    private final PersonMapper mapper;

    public PersonAdapter(PersonRepository repository, PersonMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Person> save(Person person) {
        return repository.save(mapper.toEntity(person)).map(mapper::toModel);
    }

    @Override
    public Mono<Person> findById(Long id) {
        return repository.findById(id).map(mapper::toModel);
    }

    @Override
    public Mono<Person> findByIdentification(String identificationType, String identificationNumber) {
        return repository
                .findByIdentificationTypeAndIdentificationNumber(identificationType, identificationNumber)
                .map(mapper::toModel);
    }

    @Override
    public Mono<Person> findByEmailIgnoreCase(String email) {
        return repository.findByEmailIgnoreCase(email).map(mapper::toModel);
    }
}
