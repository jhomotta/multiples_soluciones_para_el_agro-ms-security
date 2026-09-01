package com.msagro.security.drivenadapters.securitydb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

/** R2DBC mapping for the {@code user_role} table. Insert-only apart from deactivation. */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("user_role")
public class UserRoleEntity {

    @Id
    @Column("id")
    private Long id;

    @Column("user_application_id")
    private Long userApplicationId;

    @Column("role_id")
    private Long roleId;

    @Column("application_id")
    private Long applicationId;

    @Column("active")
    private Boolean active;

    @Column("valid_from")
    private OffsetDateTime validFrom;

    @Column("valid_until")
    private OffsetDateTime validUntil;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("created_by")
    private Long createdBy;
}
