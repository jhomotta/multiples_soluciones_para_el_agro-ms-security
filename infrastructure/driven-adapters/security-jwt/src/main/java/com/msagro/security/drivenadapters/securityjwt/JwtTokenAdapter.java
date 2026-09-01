package com.msagro.security.drivenadapters.securityjwt;

import com.msagro.security.model.auth.AccessToken;
import com.msagro.security.model.auth.AuthenticatedPrincipal;
import com.msagro.security.model.error.TokenException;
import com.msagro.security.usecase.config.JwtProperties;
import com.msagro.security.usecase.gateway.security.AccessTokenProviderPort;
import com.msagro.security.usecase.gateway.security.AccessTokenValidatorPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * Issues and validates short-lived HS256 access tokens.
 *
 * <p>The subject is the {@code security_user} id. The extra claims carry everything an
 * authorization decision needs — the access grant, the application, the authority list and the
 * {@code security_stamp} the token was issued against — so a protected endpoint answers without
 * touching the database.</p>
 *
 * <p>That is also the reason the token lifetime is short. An access token is a snapshot: roles
 * revoked a minute ago are still listed in a token issued two minutes ago. Revocation therefore
 * lands at the next refresh, where the grant, the account state and the stamp are all re-read —
 * and the access-token lifetime is the width of that window.</p>
 */
@Component
public class JwtTokenAdapter implements AccessTokenProviderPort, AccessTokenValidatorPort {

    private static final String CLAIM_PERSON_ID = "pid";
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_USER_APPLICATION_ID = "uaid";
    private static final String CLAIM_APPLICATION_ID = "appId";
    private static final String CLAIM_SECURITY_STAMP = "stamp";
    private static final String CLAIM_AUTHORITIES = "authorities";

    private final SecretKey signingKey;
    private final String issuer;
    private final Duration accessTtl;

    public JwtTokenAdapter(JwtProperties jwtProperties) {
        byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.getSecret());
        if (keyBytes.length < 32) {
            throw new IllegalStateException("security.jwt.secret must decode to at least 32 bytes "
                    + "(256 bits) for HS256");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = jwtProperties.getIssuer();
        this.accessTtl = Duration.ofMinutes(jwtProperties.getAccessTokenMinutes());
    }

    @Override
    public AccessToken issue(AuthenticatedPrincipal principal) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(accessTtl);

        String token = Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(principal.getSecurityUserId()))
                .claim(CLAIM_PERSON_ID, principal.getPersonId())
                .claim(CLAIM_USERNAME, principal.getUsername())
                .claim(CLAIM_USER_APPLICATION_ID, principal.getUserApplicationId())
                .claim(CLAIM_APPLICATION_ID, principal.getApplicationId())
                .claim(CLAIM_SECURITY_STAMP, principal.getSecurityStamp())
                .claim(CLAIM_AUTHORITIES, principal.getAuthorities() == null
                        ? List.of() : principal.getAuthorities())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();

        return AccessToken.builder().value(token).expiresAt(expiresAt).build();
    }

    @Override
    public AuthenticatedPrincipal validate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            @SuppressWarnings("unchecked")
            List<String> authorities = claims.get(CLAIM_AUTHORITIES, List.class);

            return AuthenticatedPrincipal.builder()
                    .securityUserId(Long.valueOf(claims.getSubject()))
                    .personId(asLong(claims.get(CLAIM_PERSON_ID)))
                    .username(claims.get(CLAIM_USERNAME, String.class))
                    .userApplicationId(asLong(claims.get(CLAIM_USER_APPLICATION_ID)))
                    .applicationId(asLong(claims.get(CLAIM_APPLICATION_ID)))
                    .securityStamp(asLong(claims.get(CLAIM_SECURITY_STAMP)))
                    .authorities(authorities == null ? List.of() : authorities)
                    .build();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new TokenException("Invalid or expired access token");
        }
    }

    /** JSON has one number type, so a claim can come back as Integer or Long. */
    private Long asLong(Object claim) {
        return claim instanceof Number number ? number.longValue() : null;
    }
}
