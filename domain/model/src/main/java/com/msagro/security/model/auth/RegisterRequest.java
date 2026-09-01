package com.msagro.security.model.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Input to register a user: creates (or reuses) the person, creates the credentials and
 * grants access to one application with the default role.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotNull
    private Long applicationId;

    @NotBlank
    @Size(max = 100)
    private String username;

    @NotBlank
    @Size(min = 12, max = 128)
    private String password;

    @NotBlank
    @Size(max = 30)
    private String identificationType;

    @NotBlank
    @Size(max = 50)
    private String identificationNumber;

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String middleName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @Size(max = 100)
    private String secondLastName;

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    @Size(max = 30)
    private String phone;

    @Size(max = 30)
    private String mobile;

    @Past
    private LocalDate birthDate;

    private Long professionId;
}
