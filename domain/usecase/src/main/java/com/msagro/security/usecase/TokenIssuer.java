package com.msagro.security.usecase;

import com.msagro.security.model.auth.AccessToken;
import com.msagro.security.model.auth.AuthResponse;
import com.msagro.security.model.auth.AuthenticatedPrincipal;
import com.msagro.security.model.auth.ClientContext;
import com.msagro.security.model.refreshtoken.RefreshToken;
import com.msagro.security.model.securityuser.SecurityUser;
import com.msagro.security.model.userapplication.UserApplication;
import com.msagro.security.usecase.config.JwtProperties;
import com.msagro.security.usecase.gateway.security.AccessTokenProviderPort;
import com.msagro.security.usecase.gateway.security.SecureTokenGeneratorPort;
import com.msagro.security.usecase.gateway.security.TokenHasherPort;
import com.msagro.security.usecase.gateway.securitydb.RefreshTokenRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.UserRoleRepositoryPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Issues a token pair for one access grant and stores the refresh-token hash. Login,
 * registration and refresh all funnel through here, so the issuing rules exist in one place.
 *
 * <p>Authorities are resolved from the grant, not from the user: the same person can hold
 * different roles in different applications.</p>
 */
@Component
public class TokenIssuer {

    private final UserRoleRepositoryPort userRoleRepository;
    private final AccessTokenProviderPort accessTokenProvider;
    private final SecureTokenGeneratorPort tokenGenerator;
    private final TokenHasherPort tokenHasher;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final JwtProperties jwtProperties;

    public TokenIssuer(UserRoleRepositoryPort userRoleRepository,
                       AccessTokenProviderPort accessTokenProvider,
                       SecureTokenGeneratorPort tokenGenerator,
                       TokenHasherPort tokenHasher,
                       RefreshTokenRepositoryPort refreshTokenRepository,
                       JwtProperties jwtProperties) {
        this.userRoleRepository = userRoleRepository;
        this.accessTokenProvider = accessTokenProvider;
        this.tokenGenerator = tokenGenerator;
        this.tokenHasher = tokenHasher;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    /** Starts a brand-new token family (login or registration). */
    public Mono<AuthResponse> issueNewSession(SecurityUser user, UserApplication grant, ClientContext context) {
        return issue(user, grant, tokenGenerator.generateTokenFamily(), context);
    }

    /** Continues an existing family (refresh rotation), keeping reuse detection meaningful. */
    public Mono<AuthResponse> issueWithinFamily(SecurityUser user,
                                                UserApplication grant,
                                                String tokenFamily,
                                                ClientContext context) {
        return issue(user, grant, tokenFamily, context);
    }

    private Mono<AuthResponse> issue(SecurityUser user,
                                     UserApplication grant,
                                     String tokenFamily,
                                     ClientContext context) {
        ClientContext ctx = context == null ? ClientContext.empty() : context;

        return userRoleRepository.findAuthorities(grant.getId())
                .collectList()
                .flatMap(authorities -> {
                    AuthenticatedPrincipal principal = AuthenticatedPrincipal.builder()
                            .securityUserId(user.getId())
                            .personId(user.getPersonId())
                            .username(user.getUsername())
                            .userApplicationId(grant.getId())
                            .applicationId(grant.getApplicationId())
                            .securityStamp(user.getSecurityStamp())
                            .authorities(authorities)
                            .build();

                    AccessToken access = accessTokenProvider.issue(principal);

                    String rawRefresh = tokenGenerator.generateToken();
                    Instant now = Instant.now();
                    Instant refreshExpiry = now.plus(Duration.ofDays(jwtProperties.getRefreshTokenDays()));

                    // Only the hash reaches the database; the raw token is returned once.
                    RefreshToken toStore = RefreshToken.builder()
                            .userApplicationId(grant.getId())
                            .tokenHash(tokenHasher.hash(rawRefresh))
                            .tokenFamily(tokenFamily)
                            .issuedAt(now)
                            .expiresAt(refreshExpiry)
                            .deviceId(ctx.getDeviceId())
                            .deviceInfo(ctx.getDeviceInfo())
                            .ipAddress(ctx.getIpAddress())
                            .userAgent(ctx.getUserAgent())
                            .build();

                    return refreshTokenRepository.save(toStore)
                            .map(saved -> AuthResponse.builder()
                                    .securityUserId(user.getId())
                                    .username(user.getUsername())
                                    .applicationId(grant.getApplicationId())
                                    .userApplicationId(grant.getId())
                                    .tokenType("Bearer")
                                    .accessToken(access.getValue())
                                    .accessTokenExpiresAt(access.getExpiresAt())
                                    .refreshToken(rawRefresh)
                                    .refreshTokenExpiresAt(refreshExpiry)
                                    .mustChangePassword(Boolean.TRUE.equals(user.getMustChangePassword()))
                                    .authorities(List.copyOf(authorities))
                                    .build());
                });
    }
}
