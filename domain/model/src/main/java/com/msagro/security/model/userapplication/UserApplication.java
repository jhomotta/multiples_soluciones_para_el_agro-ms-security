package com.msagro.security.model.userapplication;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Access grant of one user to one application, optionally limited to a time window.
 * Roles, refresh tokens and reset tokens all hang off this row. Table {@code user_application}.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserApplication {
    private Long id;
    private Long securityUserId;
    private Long applicationId;
    private Boolean active;
    private Instant accessStartAt;
    private Instant accessEndAt;
    private Instant createdAt;
    private Long createdBy;
    private Instant updatedAt;
    private Long updatedBy;

    /** True when the grant is active and {@code now} falls inside its access window. */
    public boolean isUsableAt(Instant now) {
        if (!Boolean.TRUE.equals(active)) {
            return false;
        }
        if (accessStartAt != null && accessStartAt.isAfter(now)) {
            return false;
        }
        return accessEndAt == null || accessEndAt.isAfter(now);
    }
}
