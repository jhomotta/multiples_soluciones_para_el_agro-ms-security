package com.msagro.security.drivenadapters.securitydb.adapter;

import com.msagro.security.drivenadapters.securitydb.mapper.ProfessionMapper;
import com.msagro.security.drivenadapters.securitydb.repository.ProfessionRepository;
import com.msagro.security.model.profession.Profession;
import com.msagro.security.usecase.gateway.securitydb.ProfessionRepositoryPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** R2DBC implementation of the profession output port. */
@Component
public class ProfessionAdapter implements ProfessionRepositoryPort {

    private final ProfessionRepository repository;
    private final ProfessionMapper mapper;

    public ProfessionAdapter(ProfessionRepository repository, ProfessionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Profession> save(Profession profession) {
        return repository.save(mapper.toEntity(profession)).map(mapper::toModel);
    }

    @Override
    public Mono<Profession> findById(Long id) {
        return repository.findById(id).map(mapper::toModel);
    }

    @Override
    public Mono<Profession> findByCode(String code) {
        return repository.findByCode(code).map(mapper::toModel);
    }

    @Override
    public Flux<Profession> findAllActive() {
        return repository.findByActiveTrueOrderByNameAsc().map(mapper::toModel);
    }
}
