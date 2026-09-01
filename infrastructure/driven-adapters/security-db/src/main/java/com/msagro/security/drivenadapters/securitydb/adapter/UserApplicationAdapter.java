package com.msagro.security.drivenadapters.securitydb.adapter;

import com.msagro.security.drivenadapters.securitydb.mapper.UserApplicationMapper;
import com.msagro.security.drivenadapters.securitydb.repository.UserApplicationRepository;
import com.msagro.security.model.userapplication.UserApplication;
import com.msagro.security.usecase.gateway.securitydb.UserApplicationRepositoryPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** R2DBC implementation of the access-grant output port. */
@Component
public class UserApplicationAdapter implements UserApplicationRepositoryPort {

    private final UserApplicationRepository repository;
    private final UserApplicationMapper mapper;

    public UserApplicationAdapter(UserApplicationRepository repository, UserApplicationMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<UserApplication> save(UserApplication grant) {
        return repository.save(mapper.toEntity(grant)).map(mapper::toModel);
    }

    @Override
    public Mono<UserApplication> findById(Long id) {
        return repository.findById(id).map(mapper::toModel);
    }

    @Override
    public Mono<UserApplication> findByUserAndApplication(Long securityUserId, Long applicationId) {
        return repository.findBySecurityUserIdAndApplicationId(securityUserId, applicationId)
                .map(mapper::toModel);
    }

    @Override
    public Flux<UserApplication> findByUser(Long securityUserId) {
        return repository.findBySecurityUserIdOrderByApplicationIdAsc(securityUserId).map(mapper::toModel);
    }

    @Override
    public Mono<Void> deactivate(Long id) {
        return repository.deactivate(id).then();
    }
}
