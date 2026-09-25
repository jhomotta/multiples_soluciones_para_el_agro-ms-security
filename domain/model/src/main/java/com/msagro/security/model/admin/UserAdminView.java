package com.msagro.security.model.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A user of one application, as the user administration screen shows it (HU-82). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAdminView {
    private Long securityUserId;
    private Long userApplicationId;
    private String username;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String identificationType;
    private String identificationNumber;
    private String mobile;
    @Builder.Default
    private List<String> roles = new ArrayList<>();
    /** The access grant is active and the account is enabled: the user can log in. */
    private boolean active;
    private boolean mustChangePassword;
    private Instant lastLoginAt;
    private int openSessions;
}
