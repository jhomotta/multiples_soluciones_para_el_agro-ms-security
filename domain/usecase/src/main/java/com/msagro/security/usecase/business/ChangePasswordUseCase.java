package com.msagro.security.usecase.business;

import com.msagro.security.model.auth.ChangePasswordRequest;
import com.msagro.security.model.auth.ClientContext;
import reactor.core.publisher.Mono;

/** Use case: an authenticated user replaces their own password. */
public interface ChangePasswordUseCase {

    Mono<Void> changePassword(Long securityUserId, ChangePasswordRequest request, ClientContext context);
}
