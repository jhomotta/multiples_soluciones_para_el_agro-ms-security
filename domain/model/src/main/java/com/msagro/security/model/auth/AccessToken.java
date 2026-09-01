package com.msagro.security.model.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** A freshly signed JWT access token together with the instant it expires. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessToken {
    private String value;
    private Instant expiresAt;
}
