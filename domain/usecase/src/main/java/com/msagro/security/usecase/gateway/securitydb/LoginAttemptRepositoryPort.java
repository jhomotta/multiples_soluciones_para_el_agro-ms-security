package com.msagro.security.usecase.gateway.securitydb;

import com.msagro.security.model.loginattempt.LoginAttempt;
import reactor.core.publisher.Mono;

/** Output port for {@code login_attempt}. Insert-only. */
public interface LoginAttemptRepositoryPort {

    Mono<LoginAttempt> save(LoginAttempt attempt);
}
