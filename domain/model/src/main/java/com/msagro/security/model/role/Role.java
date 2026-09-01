package com.msagro.security.model.role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** A bundle of permissions inside one application. Table {@code role}. */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    private Long id;
    private Long applicationId;
    private String code;
    private String name;
    private String description;
    private Boolean active;
    private Instant createdAt;
    private Long createdBy;
    private Instant updatedAt;
    private Long updatedBy;
}
