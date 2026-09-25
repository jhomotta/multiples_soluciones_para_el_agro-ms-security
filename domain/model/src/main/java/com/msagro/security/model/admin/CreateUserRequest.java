package com.msagro.security.model.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An administrator creates a user of their own application: the person, the credentials with
 * a temporary password, the access grant and one role.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank
    @Size(max = 100)
    private String username;

    @NotBlank
    @Size(min = 12, max = 128)
    private String temporaryPassword;

    @NotBlank
    @Size(max = 30)
    private String identificationType;

    @NotBlank
    @Size(max = 50)
    private String identificationNumber;

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    @Size(max = 30)
    private String mobile;

    @NotBlank
    @Size(max = 50)
    private String roleCode;
}
