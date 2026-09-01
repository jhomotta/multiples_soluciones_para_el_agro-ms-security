package com.msagro.security.model.profession;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Master catalogue that classifies people. Table {@code profession}. */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Profession {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Boolean active;
    private Instant createdAt;
    private Long createdBy;
    private Instant updatedAt;
    private Long updatedBy;
}
