package com.msagro.security.usecase.business;

import com.msagro.security.model.auth.AuthenticatedPrincipal;
import com.msagro.security.model.auth.ClientContext;
import com.msagro.security.model.session.SessionView;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Open sessions of an access grant, and closing one of them (HU-04, HU-82). */
public interface SessionUseCase {

    Flux<SessionView> listSessions(Long userApplicationId);

    /**
     * Closes one session: its whole token family is revoked, so it cannot refresh again. The
     * access token already issued lives until it expires or until {@link #confirmCurrent} is asked.
     */
    Mono<Void> revokeSession(Long userApplicationId, Long sessionId, String reason,
                             AuthenticatedPrincipal actor, ClientContext context);

    /**
     * Confirms that a token still stands against the database: the account is enabled, the grant
     * usable, and the security stamp the same as when the token was issued. A deactivation, a
     * password reset or a change of role therefore ends it before it expires.
     */
    Mono<AuthenticatedPrincipal> confirmCurrent(AuthenticatedPrincipal principal);
}
