package com.msagro.security.drivenadapters.securitydb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

/** R2DBC mapping for the {@code profession} table. */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("profession")
public class ProfessionEntity {

    @Id
    @Column("id")
    private Long id;

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
