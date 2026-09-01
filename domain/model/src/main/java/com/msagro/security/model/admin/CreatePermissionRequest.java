package com.msagro.security.model.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Input to create a permission inside an application. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePermissionRequest {

    @NotNull
    private Long applicationId;

    @NotBlank
    @Size(max = 120)
    private String code;

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotBlank
    @Size(max = 100)
    private String resource;

    @NotBlank
    @Size(max = 50)
    private String action;

    @Size(max = 500)
    private String description;
}
