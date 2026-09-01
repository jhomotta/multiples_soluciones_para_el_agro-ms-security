package com.msagro.security.model.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Input to log out. By default only the presented token is revoked; {@code allSessions}
 * revokes every active token of the same access grant.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequest {

    @NotBlank
    private String refreshToken;

    private boolean allSessions;
}
