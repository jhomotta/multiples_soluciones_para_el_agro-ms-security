package com.msagro.security.usecase.gateway.security;

import com.msagro.security.model.auth.AuthenticatedPrincipal;

/**
 * Output port that validates a JWT access token (signature, issuer, expiry) and returns the
 * identity it carries. Used by the reactive JWT web filter. Throws when the token is invalid.
 */
public interface AccessTokenValidatorPort {

    AuthenticatedPrincipal validate(String token);
}
