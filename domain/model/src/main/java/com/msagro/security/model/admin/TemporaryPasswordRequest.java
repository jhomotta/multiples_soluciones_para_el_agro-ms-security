package com.msagro.security.model.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A password the administrator gives; the user must change it at the next login. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemporaryPasswordRequest {

    @NotBlank
    @Size(min = 12, max = 128)
    private String temporaryPassword;
}
