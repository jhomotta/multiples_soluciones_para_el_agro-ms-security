package com.msagro.security.model.rolepermission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Permission granted to a role. Composite foreign keys keep both sides inside the same
 * application. Table {@code role_permission}.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class RolePermission {
    private Long id;
    private Long roleId;
    private Long permissionId;
    private Long applicationId;
    private Instant createdAt;
    private Long createdBy;
}
