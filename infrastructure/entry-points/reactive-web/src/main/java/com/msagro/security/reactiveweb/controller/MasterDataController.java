package com.msagro.security.reactiveweb.controller;

import com.msagro.security.model.admin.CreateApplicationRequest;
import com.msagro.security.model.admin.CreateCompanyRequest;
import com.msagro.security.model.admin.CreatePermissionRequest;
import com.msagro.security.model.admin.CreateProfessionRequest;
import com.msagro.security.model.admin.CreateRoleRequest;
import com.msagro.security.model.admin.GrantPermissionRequest;
import com.msagro.security.model.application.Application;
import com.msagro.security.model.auth.AuthenticatedPrincipal;
import com.msagro.security.model.company.Company;
import com.msagro.security.model.generic.GenericResponse;
import com.msagro.security.model.permission.Permission;
import com.msagro.security.model.profession.Profession;
import com.msagro.security.model.role.Role;
import com.msagro.security.model.rolepermission.RolePermission;
import com.msagro.security.usecase.business.MasterDataUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Master catalogues: companies, applications, professions, roles and permissions.
 *
 * <p>Each route is gated by the permission code it needs, checked against the authority list the
 * access token carries. The codes are the ones seeded into the {@code permission} table, so
 * adding a role that may create applications is a data change, not a code change.</p>
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Master data", description = "Companies, applications, professions, roles and permissions")
public class MasterDataController {

    private final MasterDataUseCase masterDataUseCase;

    // ── companies ────────────────────────────────────────────────────────────

    @PostMapping("/companies")
    @PreAuthorize("hasAuthority('COMPANY_CREATE')")
    @Operation(summary = "Create a company")
    public Mono<ResponseEntity<GenericResponse<Company>>> createCompany(
            @Valid @RequestBody CreateCompanyRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal actor) {
        return masterDataUseCase.createCompany(request, actor)
                .map(company -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(GenericResponse.success(company, "Company created", 201)));
    }

    @GetMapping("/companies")
    @PreAuthorize("hasAuthority('COMPANY_READ')")
    @Operation(summary = "List the active companies")
    public Mono<ResponseEntity<GenericResponse<List<Company>>>> listCompanies() {
        return masterDataUseCase.listCompanies().collectList()
                .map(companies -> ResponseEntity.ok(GenericResponse.success(companies, "OK", 200)));
    }

    // ── applications ─────────────────────────────────────────────────────────

    @PostMapping("/applications")
    @PreAuthorize("hasAuthority('APPLICATION_CREATE')")
    @Operation(summary = "Create an application inside a company")
    public Mono<ResponseEntity<GenericResponse<Application>>> createApplication(
            @Valid @RequestBody CreateApplicationRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal actor) {
        return masterDataUseCase.createApplication(request, actor)
                .map(application -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(GenericResponse.success(application, "Application created", 201)));
    }

    @GetMapping("/companies/{companyId}/applications")
    @PreAuthorize("hasAuthority('APPLICATION_READ')")
    @Operation(summary = "List the applications of a company")
    public Mono<ResponseEntity<GenericResponse<List<Application>>>> listApplications(
            @PathVariable Long companyId) {
        return masterDataUseCase.listApplications(companyId).collectList()
                .map(applications -> ResponseEntity.ok(GenericResponse.success(applications, "OK", 200)));
    }

    // ── professions ──────────────────────────────────────────────────────────

    @PostMapping("/professions")
    @PreAuthorize("hasAuthority('PROFESSION_CREATE')")
    @Operation(summary = "Create a profession")
    public Mono<ResponseEntity<GenericResponse<Profession>>> createProfession(
            @Valid @RequestBody CreateProfessionRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal actor) {
        return masterDataUseCase.createProfession(request, actor)
                .map(profession -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(GenericResponse.success(profession, "Profession created", 201)));
    }

    @GetMapping("/professions")
    @PreAuthorize("hasAuthority('PROFESSION_READ')")
    @Operation(summary = "List the active professions")
    public Mono<ResponseEntity<GenericResponse<List<Profession>>>> listProfessions() {
        return masterDataUseCase.listProfessions().collectList()
                .map(professions -> ResponseEntity.ok(GenericResponse.success(professions, "OK", 200)));
    }

    // ── roles ────────────────────────────────────────────────────────────────

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    @Operation(summary = "Create a role inside an application")
    public Mono<ResponseEntity<GenericResponse<Role>>> createRole(
            @Valid @RequestBody CreateRoleRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal actor) {
        return masterDataUseCase.createRole(request, actor)
                .map(role -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(GenericResponse.success(role, "Role created", 201)));
    }

    @GetMapping("/applications/{applicationId}/roles")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    @Operation(summary = "List the roles of an application")
    public Mono<ResponseEntity<GenericResponse<List<Role>>>> listRoles(@PathVariable Long applicationId) {
        return masterDataUseCase.listRoles(applicationId).collectList()
                .map(roles -> ResponseEntity.ok(GenericResponse.success(roles, "OK", 200)));
    }

    // ── permissions ──────────────────────────────────────────────────────────

    @PostMapping("/permissions")
    @PreAuthorize("hasAuthority('PERMISSION_CREATE')")
    @Operation(summary = "Create a permission inside an application")
    public Mono<ResponseEntity<GenericResponse<Permission>>> createPermission(
            @Valid @RequestBody CreatePermissionRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal actor) {
        return masterDataUseCase.createPermission(request, actor)
                .map(permission -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(GenericResponse.success(permission, "Permission created", 201)));
    }

    @GetMapping("/applications/{applicationId}/permissions")
    @PreAuthorize("hasAuthority('PERMISSION_READ')")
    @Operation(summary = "List the permissions of an application")
    public Mono<ResponseEntity<GenericResponse<List<Permission>>>> listPermissions(
            @PathVariable Long applicationId) {
        return masterDataUseCase.listPermissions(applicationId).collectList()
                .map(permissions -> ResponseEntity.ok(GenericResponse.success(permissions, "OK", 200)));
    }

    @PostMapping("/roles/permissions")
    @PreAuthorize("hasAuthority('PERMISSION_GRANT')")
    @Operation(summary = "Grant a permission to a role of the same application")
    public Mono<ResponseEntity<GenericResponse<RolePermission>>> grantPermission(
            @Valid @RequestBody GrantPermissionRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal actor) {
        return masterDataUseCase.grantPermissionToRole(request, actor)
                .map(granted -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(GenericResponse.success(granted, "Permission granted to role", 201)));
    }
}
