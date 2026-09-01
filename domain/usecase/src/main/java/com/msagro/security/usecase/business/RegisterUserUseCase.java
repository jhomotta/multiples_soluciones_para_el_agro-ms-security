package com.msagro.security.usecase.business;

import com.msagro.security.model.auth.AuthResponse;
import com.msagro.security.model.auth.ClientContext;
import com.msagro.security.model.auth.RegisterRequest;
import reactor.core.publisher.Mono;

/** Use case: register a user, grant it access to one application and return a token pair. */
public interface RegisterUserUseCase {

    Mono<AuthResponse> register(RegisterRequest request, ClientContext context);
}
