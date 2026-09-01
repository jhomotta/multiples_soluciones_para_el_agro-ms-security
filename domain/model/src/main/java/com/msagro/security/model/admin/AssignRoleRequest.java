package com.msagro.security.model.admin;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Input to assign a role to an access grant. The role must belong to the same application as
 * the grant — the database enforces it through composite foreign keys.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignRoleRequest {

    @NotNull
    private Long userApplicationId;

    @NotNull
    private Long roleId;

    private Instant validFrom;

    private Instant validUntil;
}
