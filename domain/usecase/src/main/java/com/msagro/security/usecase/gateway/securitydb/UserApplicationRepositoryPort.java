package com.msagro.security.usecase.gateway.securitydb;

import com.msagro.security.model.userapplication.UserApplication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Output port for {@code user_application} access grants. */
public interface UserApplicationRepositoryPort {

    Mono<UserApplication> save(UserApplication grant);

    Mono<UserApplication> findById(Long id);

    Mono<UserApplication> findByUserAndApplication(Long securityUserId, Long applicationId);

    Flux<UserApplication> findByUser(Long securityUserId);

    /** Every grant of an application, active or not. */
    Flux<UserApplication> findByApplication(Long applicationId);

    /** Turns a deactivated grant on again. */
    Mono<Void> activate(Long id);

    /** Deactivates the grant; the rows stay for audit purposes. */
    Mono<Void> deactivate(Long id);
}
