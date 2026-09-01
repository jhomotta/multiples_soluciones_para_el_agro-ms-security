package com.msagro.security.model.userrole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Role assigned to an access grant, optionally limited in time. {@code applicationId} is
 * carried along so the database can enforce, through composite foreign keys, that the role
 * belongs to the same application as the grant. Table {@code user_role}.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserRole {
    private Long id;
    private Long userApplicationId;
    private Long roleId;
    private Long applicationId;
    private Boolean active;
    private Instant validFrom;
    private Instant validUntil;
    private Instant createdAt;
    private Long createdBy;
}
