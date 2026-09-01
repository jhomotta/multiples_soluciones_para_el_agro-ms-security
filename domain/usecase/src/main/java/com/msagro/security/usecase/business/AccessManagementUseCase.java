package com.msagro.security.usecase.business;

import com.msagro.security.model.admin.AssignRoleRequest;
import com.msagro.security.model.admin.GrantAccessRequest;
import com.msagro.security.model.admin.UserAccessView;
import com.msagro.security.model.auth.AuthenticatedPrincipal;
import com.msagro.security.model.auth.ClientContext;
import com.msagro.security.model.userapplication.UserApplication;
import com.msagro.security.model.userrole.UserRole;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Use case: administer who may enter which application, and with which roles. */
public interface AccessManagementUseCase {

    Mono<UserApplication> grantAccess(GrantAccessRequest request, AuthenticatedPrincipal actor, ClientContext context);

    Mono<Void> revokeAccess(Long userApplicationId, AuthenticatedPrincipal actor, ClientContext context);

    Mono<UserRole> assignRole(AssignRoleRequest request, AuthenticatedPrincipal actor, ClientContext context);

    Mono<Void> revokeRole(Long userApplicationId, Long roleId, AuthenticatedPrincipal actor, ClientContext context);

    Mono<UserAccessView> describeAccess(Long userApplicationId);

    Flux<UserAccessView> listAccessOfUser(Long securityUserId);
}
