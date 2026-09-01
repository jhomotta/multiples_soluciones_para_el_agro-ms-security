package com.msagro.security.model.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/** Result of a successful registration, login or refresh: the new token pair plus context. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private Long securityUserId;
    private String username;
    private Long applicationId;
    private Long userApplicationId;
    private String tokenType;
    private String accessToken;
    private Instant accessTokenExpiresAt;
    private String refreshToken;
    private Instant refreshTokenExpiresAt;
    /** True when the caller must change the password before doing anything else. */
    private Boolean mustChangePassword;
    private List<String> authorities;
}
