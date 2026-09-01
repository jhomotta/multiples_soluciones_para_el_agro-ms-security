package com.msagro.security.usecase.gateway.security;

import com.msagro.security.model.auth.AccessToken;
import com.msagro.security.model.auth.AuthenticatedPrincipal;

/**
 * Output port that issues short-lived signed JWT access tokens. Implemented by the
 * security-jwt driven adapter.
 */
public interface AccessTokenProviderPort {

    AccessToken issue(AuthenticatedPrincipal principal);
}
