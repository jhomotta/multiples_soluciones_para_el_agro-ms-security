package com.msagro.security.drivenadapters.securitydb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

/** R2DBC mapping for the {@code security_user} table. */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("security_user")
public class SecurityUserEntity {

    @Id
    @Column("id")
    private Long id;

    @Column("person_id")
    private Long personId;

    @Column("username")
    private String username;

    @Column("password_hash")
    private String passwordHash;

    @Column("enabled")
    private Boolean enabled;

    @Column("locked")
    private Boolean locked;

    @Column("locked_until")
    private OffsetDateTime lockedUntil;

    /** SMALLINT in PostgreSQL. */
    @Column("failed_attempts")
    private Short failedAttempts;

    @Column("last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column("password_changed_at")
    private OffsetDateTime passwordChangedAt;

    @Column("credentials_expired")
    private Boolean credentialsExpired;

    @Column("account_expires_at")
    private OffsetDateTime accountExpiresAt;

    @Column("must_change_password")
    private Boolean mustChangePassword;

    @Column("security_stamp")
    private Long securityStamp;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("created_by")
    private Long createdBy;

    @Column("updated_at")
    private OffsetDateTime updatedAt;

    @Column("updated_by")
    private Long updatedBy;
}
