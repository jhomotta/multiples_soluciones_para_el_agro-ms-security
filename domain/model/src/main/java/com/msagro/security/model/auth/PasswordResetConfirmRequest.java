package com.msagro.security.model.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Input to finish a password reset with the single-use token. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetConfirmRequest {

    @NotBlank
    private String resetToken;

    @NotBlank
    @Size(min = 12, max = 128)
    private String newPassword;
}
