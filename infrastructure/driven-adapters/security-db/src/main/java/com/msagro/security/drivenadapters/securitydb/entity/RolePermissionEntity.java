package com.msagro.security.drivenadapters.securitydb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

/** R2DBC mapping for the {@code role_permission} table. Insert-only. */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("role_permission")
public class RolePermissionEntity {

    @Id
    @Column("id")
    private Long id;

    @Column("role_id")
    private Long roleId;

    @Column("permission_id")
    private Long permissionId;

    @Column("application_id")
    private Long applicationId;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("created_by")
    private Long createdBy;
}
