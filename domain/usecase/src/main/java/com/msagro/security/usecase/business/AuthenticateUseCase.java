package com.msagro.security.usecase.business;

import com.msagro.security.model.auth.AuthResponse;
import com.msagro.security.model.auth.ClientContext;
import com.msagro.security.model.auth.LoginRequest;
import reactor.core.publisher.Mono;

/** Use case: authenticate a user against one application. */
public interface AuthenticateUseCase {

    Mono<AuthResponse> login(LoginRequest request, ClientContext context);
}
