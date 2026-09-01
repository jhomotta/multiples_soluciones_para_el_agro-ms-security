package com.msagro.security.model.admin;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Input to grant a user access to an application, optionally inside a time window. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrantAccessRequest {

    @NotNull
    private Long securityUserId;

    @NotNull
    private Long applicationId;

    private Instant accessStartAt;

    private Instant accessEndAt;
}
