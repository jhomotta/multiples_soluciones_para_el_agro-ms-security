package com.msagro.security.drivenadapters.securitydb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

/** R2DBC mapping for the {@code application} table. */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("application")
public class ApplicationEntity {

    @Id
    @Column("id")
    private Long id;

    @Column("company_id")
    private Long companyId;

    @Column("code")
    private String code;

    @Column("name")
    private String name;

    @Column("description")
    private String description;

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
