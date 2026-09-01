package com.msagro.security.model.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Application owned by a company. Roles, permissions and user access are always scoped
 * to one application. Table {@code application}.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Application {
    private Long id;
    private Long companyId;
    private String code;
    private String name;
    private String description;
    private Boolean active;
    private Instant createdAt;
    private Long createdBy;
    private Instant updatedAt;
    private Long updatedBy;
}
