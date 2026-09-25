package com.msagro.security.usecase.business;

import com.msagro.security.model.admin.CreateUserRequest;
import com.msagro.security.model.admin.UpdateUserRequest;
import com.msagro.security.model.admin.UserAdminView;
import com.msagro.security.model.auth.AuthenticatedPrincipal;
import com.msagro.security.model.auth.ClientContext;
import com.msagro.security.model.role.Role;
import com.msagro.security.model.session.SessionView;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Users of one application, managed by its administrator (HU-82). Every operation works on the
 * application of the caller's token: an administrator never reaches the users of another one.
 */
public interface UserAdministrationUseCase {

    Flux<Role> roles(AuthenticatedPrincipal actor);

    Flux<UserAdminView> list(AuthenticatedPrincipal actor);

    Mono<UserAdminView> create(CreateUserRequest request, AuthenticatedPrincipal actor, ClientContext context);

    Mono<UserAdminView> update(Long securityUserId, UpdateUserRequest request,
                               AuthenticatedPrincipal actor, ClientContext context);

    Mono<Void> deactivate(Long securityUserId, String reason, AuthenticatedPrincipal actor, ClientContext context);

    Mono<Void> activate(Long securityUserId, AuthenticatedPrincipal actor, ClientContext context);

    Mono<Void> resetPassword(Long securityUserId, String temporaryPassword,
                             AuthenticatedPrincipal actor, ClientContext context);

    Flux<SessionView> sessions(Long securityUserId, AuthenticatedPrincipal actor);

    Mono<Void> revokeSession(Long securityUserId, Long sessionId, String reason,
                             AuthenticatedPrincipal actor, ClientContext context);
}
