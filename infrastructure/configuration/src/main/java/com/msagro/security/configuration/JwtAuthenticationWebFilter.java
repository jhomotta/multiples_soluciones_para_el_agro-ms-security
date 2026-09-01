package com.msagro.security.configuration;

import com.msagro.security.model.auth.AuthenticatedPrincipal;
import com.msagro.security.usecase.gateway.security.AccessTokenValidatorPort;
import com.msagro.security.utility.AuthConstants;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Reads {@code Authorization: Bearer <token>} and, when the access token validates, puts an
 * {@link AuthenticatedPrincipal} with its authorities into the reactive security context.
 *
 * <p>An invalid or expired token is not answered here. The request simply continues
 * unauthenticated, and the filter chain returns 401 for a protected route and serves a public
 * one normally — which is what lets a client with a stale token still reach
 * {@code /api/v1/auth/refresh}.</p>
 *
 * <p>Authentication is stateless: no session is created, and the authority list comes from the
 * token rather than from a database read on every request.</p>
 */
@Component
public class JwtAuthenticationWebFilter implements WebFilter {

    private final AccessTokenValidatorPort tokenValidator;

    public JwtAuthenticationWebFilter(AccessTokenValidatorPort tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(AuthConstants.BEARER_PREFIX)) {
            return chain.filter(exchange);
        }
        String token = header.substring(AuthConstants.BEARER_PREFIX.length());

        return Mono.fromCallable(() -> tokenValidator.validate(token))
                .map(this::toAuthentication)
                .flatMap(authentication -> chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication)))
                .onErrorResume(error -> chain.filter(exchange));
    }

    private Authentication toAuthentication(AuthenticatedPrincipal principal) {
        List<SimpleGrantedAuthority> authorities = principal.getAuthorities().stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }
}
