package com.msagro.security.model.loginattempt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * One authentication attempt, successful or not. {@code securityUserId} is null when the
 * username did not exist — those attempts matter most. Table {@code login_attempt}.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class LoginAttempt {
    private Long id;
    private Long securityUserId;
    private Long applicationId;
    private String usernameAttempted;
    private Boolean success;
    private String ipAddress;
    private String userAgent;
    private String failureReason;
    private Instant attemptedAt;
}
