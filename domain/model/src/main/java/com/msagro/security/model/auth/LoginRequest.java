package com.msagro.security.model.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Input to log in. A login is always scoped to one application: the user needs an active
 * {@code user_application} grant for it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotNull
    private Long applicationId;

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    /** Optional stable identifier of the device, stored with the refresh token. */
    private String deviceId;
}
