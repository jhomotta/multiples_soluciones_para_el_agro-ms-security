package com.msagro.security.usecase.business;

import com.msagro.security.model.auth.ClientContext;
import com.msagro.security.model.auth.LogoutRequest;
import reactor.core.publisher.Mono;

/** Use case: revoke the presented refresh token, or every token of the same access grant. */
public interface LogoutUseCase {

    Mono<Void> logout(LogoutRequest request, ClientContext context);
}
