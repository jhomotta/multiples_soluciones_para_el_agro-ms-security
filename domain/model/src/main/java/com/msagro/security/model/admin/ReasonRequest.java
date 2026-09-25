package com.msagro.security.model.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** The reason of an action that must say why: revoking a session, deactivating a user. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReasonRequest {

    @NotBlank
    @Size(min = 3, max = 300)
    private String reason;
}
