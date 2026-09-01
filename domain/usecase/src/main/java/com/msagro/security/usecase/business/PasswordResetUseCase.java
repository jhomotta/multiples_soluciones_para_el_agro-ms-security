package com.msagro.security.usecase.business;

import com.msagro.security.model.auth.ClientContext;
import com.msagro.security.model.auth.PasswordResetConfirmRequest;
import com.msagro.security.model.auth.PasswordResetRequest;
import com.msagro.security.model.auth.PasswordResetTicket;
import reactor.core.publisher.Mono;

/** Use case: the two halves of a self-service password reset. */
public interface PasswordResetUseCase {

    /** Issues a single-use token. Answers identically whether or not the user exists. */
    Mono<PasswordResetTicket> request(PasswordResetRequest request, ClientContext context);

    /** Consumes the token, sets the new password and revokes every session of the grant. */
    Mono<Void> confirm(PasswordResetConfirmRequest request, ClientContext context);
}
