package com.msagro.security.model.admin;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Input to grant a permission to a role, both inside the same application. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrantPermissionRequest {

    @NotNull
    private Long roleId;

    @NotNull
    private Long permissionId;
}
