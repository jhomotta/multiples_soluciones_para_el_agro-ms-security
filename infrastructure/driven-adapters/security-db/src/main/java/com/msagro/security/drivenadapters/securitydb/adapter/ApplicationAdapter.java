package com.msagro.security.drivenadapters.securitydb.adapter;

import com.msagro.security.drivenadapters.securitydb.mapper.ApplicationMapper;
import com.msagro.security.drivenadapters.securitydb.repository.ApplicationRepository;
import com.msagro.security.model.application.Application;
import com.msagro.security.usecase.gateway.securitydb.ApplicationRepositoryPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** R2DBC implementation of the application output port. */
@Component
public class ApplicationAdapter implements ApplicationRepositoryPort {

    private final ApplicationRepository repository;
    private final ApplicationMapper mapper;

    public ApplicationAdapter(ApplicationRepository repository, ApplicationMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Application> save(Application application) {
        return repository.save(mapper.toEntity(application)).map(mapper::toModel);
    }

    @Override
    public Mono<Application> findById(Long id) {
        return repository.findById(id).map(mapper::toModel);
    }

    @Override
    public Mono<Application> findByCompanyAndCode(Long companyId, String code) {
        return repository.findByCompanyIdAndCode(companyId, code).map(mapper::toModel);
    }

    @Override
    public Flux<Application> findByCompany(Long companyId) {
        return repository.findByCompanyIdOrderByCodeAsc(companyId).map(mapper::toModel);
    }
}
