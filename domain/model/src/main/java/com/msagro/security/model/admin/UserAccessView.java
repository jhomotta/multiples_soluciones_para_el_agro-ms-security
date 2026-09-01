package com.msagro.security.model.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/** Read model describing one user's access to one application, with the roles it carries. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccessView {
    private Long userApplicationId;
    private Long securityUserId;
    private String username;
    private String fullName;
    private String email;
    private Long applicationId;
    private String applicationCode;
    private Boolean active;
    private Instant accessStartAt;
    private Instant accessEndAt;
    private List<String> roles;
    private List<String> permissions;
}
