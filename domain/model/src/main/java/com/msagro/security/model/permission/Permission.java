package com.msagro.security.model.permission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * A fine-grained right inside one application, expressed as {@code resource} + {@code action}
 * (for example {@code PERSON} + {@code CREATE}). {@code code} is the stable string used in
 * authorization checks. Table {@code permission}.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Permission {
    private Long id;
    private Long applicationId;
    private String code;
    private String name;
    private String description;
    private String resource;
    private String action;
    private Boolean active;
    private Instant createdAt;
    private Long createdBy;
    private Instant updatedAt;
    private Long updatedBy;
}
