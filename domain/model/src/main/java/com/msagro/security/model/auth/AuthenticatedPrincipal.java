package com.msagro.security.model.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Identity carried by a validated access token. The JWT filter places it into the reactive
 * security context, so protected endpoints authorize without touching the database.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticatedPrincipal {
    private Long securityUserId;
    private Long personId;
    private String username;
    private Long userApplicationId;
    private Long applicationId;
    /** Copy of {@code security_user.security_stamp} at issuing time. */
    private Long securityStamp;
    private List<String> authorities;
}
