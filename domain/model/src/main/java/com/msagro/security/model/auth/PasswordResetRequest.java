package com.msagro.security.model.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Input to start a password reset. The response is always the same whether or not the user
 * exists, so the endpoint cannot be used to enumerate accounts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequest {

    @NotNull
    private Long applicationId;

    @NotBlank
    private String username;
}
