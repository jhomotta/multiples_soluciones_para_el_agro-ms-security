package com.msagro.security.model.person;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Personal data of a real human being. A person can exist without credentials;
 * the credentials live in {@code security_user}. Table {@code person}.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Person {
    private Long id;
    private Long professionId;
    private String identificationType;
    private String identificationNumber;
    private String firstName;
    private String middleName;
    private String lastName;
    private String secondLastName;
    private String email;
    private String phone;
    private String mobile;
    private LocalDate birthDate;
    private Boolean active;
    private Instant createdAt;
    private Long createdBy;
    private Instant updatedAt;
    private Long updatedBy;

    /** Full name built from the four optional name parts, without double spaces. */
    public String fullName() {
        return java.util.stream.Stream.of(firstName, middleName, lastName, secondLastName)
                .filter(part -> part != null && !part.isBlank())
                .reduce((a, b) -> a + " " + b)
                .orElse("");
    }
}
