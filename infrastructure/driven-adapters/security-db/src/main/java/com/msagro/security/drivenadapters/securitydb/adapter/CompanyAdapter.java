package com.msagro.security.drivenadapters.securitydb.adapter;

import com.msagro.security.drivenadapters.securitydb.mapper.CompanyMapper;
import com.msagro.security.drivenadapters.securitydb.repository.CompanyRepository;
import com.msagro.security.model.company.Company;
import com.msagro.security.usecase.gateway.securitydb.CompanyRepositoryPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** R2DBC implementation of the company output port. */
@Component
public class CompanyAdapter implements CompanyRepositoryPort {

    private final CompanyRepository repository;
    private final CompanyMapper mapper;

    public CompanyAdapter(CompanyRepository repository, CompanyMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Company> save(Company company) {
        return repository.save(mapper.toEntity(company)).map(mapper::toModel);
    }

    @Override
    public Mono<Company> findById(Long id) {
        return repository.findById(id).map(mapper::toModel);
    }

    @Override
    public Mono<Company> findByCode(String code) {
        return repository.findByCode(code).map(mapper::toModel);
    }

    @Override
    public Flux<Company> findAllActive() {
        return repository.findByActiveTrueOrderByCodeAsc().map(mapper::toModel);
    }
}
