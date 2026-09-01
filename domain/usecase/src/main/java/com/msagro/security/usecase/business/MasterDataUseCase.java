package com.msagro.security.usecase.business;

import com.msagro.security.model.admin.CreateApplicationRequest;
import com.msagro.security.model.admin.CreateCompanyRequest;
import com.msagro.security.model.admin.CreatePermissionRequest;
import com.msagro.security.model.admin.CreateProfessionRequest;
import com.msagro.security.model.admin.CreateRoleRequest;
import com.msagro.security.model.admin.GrantPermissionRequest;
import com.msagro.security.model.application.Application;
import com.msagro.security.model.auth.AuthenticatedPrincipal;
import com.msagro.security.model.company.Company;
import com.msagro.security.model.permission.Permission;
import com.msagro.security.model.profession.Profession;
import com.msagro.security.model.role.Role;
import com.msagro.security.model.rolepermission.RolePermission;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Use case: maintain the master catalogues — companies, applications, professions, RBAC. */
public interface MasterDataUseCase {

    Mono<Company> createCompany(CreateCompanyRequest request, AuthenticatedPrincipal actor);

    Flux<Company> listCompanies();

    Mono<Application> createApplication(CreateApplicationRequest request, AuthenticatedPrincipal actor);

    Flux<Application> listApplications(Long companyId);

    Mono<Profession> createProfession(CreateProfessionRequest request, AuthenticatedPrincipal actor);

    Flux<Profession> listProfessions();

    Mono<Role> createRole(CreateRoleRequest request, AuthenticatedPrincipal actor);

    Flux<Role> listRoles(Long applicationId);

    Mono<Permission> createPermission(CreatePermissionRequest request, AuthenticatedPrincipal actor);

    Flux<Permission> listPermissions(Long applicationId);

    Mono<RolePermission> grantPermissionToRole(GrantPermissionRequest request, AuthenticatedPrincipal actor);
}
