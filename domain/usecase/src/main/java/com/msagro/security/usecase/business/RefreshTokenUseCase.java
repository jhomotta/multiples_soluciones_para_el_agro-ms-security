package com.msagro.security.usecase.business;

import com.msagro.security.model.auth.AuthResponse;
import com.msagro.security.model.auth.ClientContext;
import reactor.core.publisher.Mono;

/** Use case: rotate a valid refresh token into a new token pair. */
public interface RefreshTokenUseCase {

    Mono<AuthResponse> refresh(String refreshToken, ClientContext context);
}
