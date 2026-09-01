package com.msagro.security.drivenadapters.securitydb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

/** R2DBC mapping for the {@code user_application} table. */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("user_application")
public class UserApplicationEntity {

    @Id
    @Column("id")
    private Long id;

    @Column("security_user_id")
    private Long securityUserId;

    @Column("application_id")
    private Long applicationId;

    @Column("access_start_at")
    private OffsetDateTime accessStartAt;

    @Column("access_end_at")
    private OffsetDateTime accessEndAt;

    @Column("active")
    private Boolean active;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("created_by")
    private Long createdBy;

    @Column("updated_at")
    private OffsetDateTime updatedAt;

    @Column("updated_by")
    private Long updatedBy;
}
