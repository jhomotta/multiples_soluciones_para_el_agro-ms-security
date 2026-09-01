package com.msagro.security.usecase;

import com.msagro.security.model.admin.CreateApplicationRequest;
import com.msagro.security.model.admin.CreateCompanyRequest;
import com.msagro.security.model.admin.CreatePermissionRequest;
import com.msagro.security.model.admin.CreateProfessionRequest;
import com.msagro.security.model.admin.CreateRoleRequest;
import com.msagro.security.model.admin.GrantPermissionRequest;
import com.msagro.security.model.application.Application;
import com.msagro.security.model.auth.AuthenticatedPrincipal;
import com.msagro.security.model.company.Company;
import com.msagro.security.model.enums.AuditEventType;
import com.msagro.security.model.error.BusinessException;
import com.msagro.security.model.error.ConflictException;
import com.msagro.security.model.error.NotFoundException;
import com.msagro.security.model.permission.Permission;
import com.msagro.security.model.profession.Profession;
import com.msagro.security.model.role.Role;
import com.msagro.security.model.rolepermission.RolePermission;
import com.msagro.security.usecase.business.MasterDataUseCase;
import com.msagro.security.usecase.gateway.securitydb.ApplicationRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.CompanyRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.PermissionRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.ProfessionRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.RolePermissionRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.RoleRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Locale;

/**
 * Maintenance of the master catalogues: companies, applications, professions, roles and
 * permissions, plus wiring permissions into roles.
 *
 * <p>Codes are normalised to upper case before they are stored, because every unique constraint
 * in the schema is on the raw code — normalising here is what makes {@code agro_core} and
 * {@code AGRO_CORE} the same thing rather than two rows nobody can tell apart.</p>
 */
@Service
public class MasterDataUseCaseImpl implements MasterDataUseCase {

    private final CompanyRepositoryPort companyRepository;
    private final ApplicationRepositoryPort applicationRepository;
    private final ProfessionRepositoryPort professionRepository;
    private final RoleRepositoryPort roleRepository;
    private final PermissionRepositoryPort permissionRepository;
    private final RolePermissionRepositoryPort rolePermissionRepository;
    private final AuditRecorder auditRecorder;

    public MasterDataUseCaseImpl(CompanyRepositoryPort companyRepository,
                                 ApplicationRepositoryPort applicationRepository,
                                 ProfessionRepositoryPort professionRepository,
                                 RoleRepositoryPort roleRepository,
                                 PermissionRepositoryPort permissionRepository,
                                 RolePermissionRepositoryPort rolePermissionRepository,
                                 AuditRecorder auditRecorder) {
        this.companyRepository = companyRepository;
        this.applicationRepository = applicationRepository;
        this.professionRepository = professionRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.auditRecorder = auditRecorder;
    }

    // ── company ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Mono<Company> createCompany(CreateCompanyRequest request, AuthenticatedPrincipal actor) {
        String code = normalise(request.getCode());

        return companyRepository.findByCode(code)
                .flatMap(existing -> Mono.<Company>error(
                        new ConflictException("Company code already in use: " + code)))
                .switchIfEmpty(Mono.defer(() -> companyRepository.save(Company.builder()
                        .code(code)
                        .name(request.getName())
                        .description(request.getDescription())
                        .active(true)
                        .createdAt(Instant.now())
                        .createdBy(actorId(actor))
                        .build())))
                .flatMap(saved -> audit(actor, null, "COMPANY", saved.getId(), saved.getCode())
                        .thenReturn(saved));
    }

    @Override
    public Flux<Company> listCompanies() {
        return companyRepository.findAllActive();
    }

    // ── application ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Mono<Application> createApplication(CreateApplicationRequest request, AuthenticatedPrincipal actor) {
        String code = normalise(request.getCode());

        return companyRepository.findById(request.getCompanyId())
                .switchIfEmpty(Mono.error(new NotFoundException("Company", request.getCompanyId())))
                .then(applicationRepository.findByCompanyAndCode(request.getCompanyId(), code))
                .flatMap(existing -> Mono.<Application>error(new ConflictException(
                        "Application code already in use in this company: " + code)))
                .switchIfEmpty(Mono.defer(() -> applicationRepository.save(Application.builder()
                        .companyId(request.getCompanyId())
                        .code(code)
                        .name(request.getName())
                        .description(request.getDescription())
                        .active(true)
                        .createdAt(Instant.now())
                        .createdBy(actorId(actor))
                        .build())))
                .flatMap(saved -> audit(actor, saved.getId(), "APPLICATION", saved.getId(), saved.getCode())
                        .thenReturn(saved));
    }

    @Override
    public Flux<Application> listApplications(Long companyId) {
        return applicationRepository.findByCompany(companyId);
    }

    // ── profession ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Mono<Profession> createProfession(CreateProfessionRequest request, AuthenticatedPrincipal actor) {
        String code = normalise(request.getCode());

        return professionRepository.findByCode(code)
                .flatMap(existing -> Mono.<Profession>error(
                        new ConflictException("Profession code already in use: " + code)))
                .switchIfEmpty(Mono.defer(() -> professionRepository.save(Profession.builder()
                        .code(code)
                        .name(request.getName())
                        .description(request.getDescription())
                        .active(true)
                        .createdAt(Instant.now())
                        .createdBy(actorId(actor))
                        .build())))
                .flatMap(saved -> audit(actor, null, "PROFESSION", saved.getId(), saved.getCode())
                        .thenReturn(saved));
    }

    @Override
    public Flux<Profession> listProfessions() {
        return professionRepository.findAllActive();
    }

    // ── role ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Mono<Role> createRole(CreateRoleRequest request, AuthenticatedPrincipal actor) {
        String code = normalise(request.getCode());

        return applicationRepository.findById(request.getApplicationId())
                .switchIfEmpty(Mono.error(new NotFoundException("Application", request.getApplicationId())))
                .then(roleRepository.findByApplicationAndCode(request.getApplicationId(), code))
                .flatMap(existing -> Mono.<Role>error(new ConflictException(
                        "Role code already in use in this application: " + code)))
                .switchIfEmpty(Mono.defer(() -> roleRepository.save(Role.builder()
                        .applicationId(request.getApplicationId())
                        .code(code)
                        .name(request.getName())
                        .description(request.getDescription())
                        .active(true)
                        .createdAt(Instant.now())
                        .createdBy(actorId(actor))
                        .build())))
                .flatMap(saved -> audit(actor, saved.getApplicationId(), "ROLE", saved.getId(), saved.getCode())
                        .thenReturn(saved));
    }

    @Override
    public Flux<Role> listRoles(Long applicationId) {
        return roleRepository.findByApplication(applicationId);
    }

    // ── permission ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Mono<Permission> createPermission(CreatePermissionRequest request, AuthenticatedPrincipal actor) {
        String code = normalise(request.getCode());

        return applicationRepository.findById(request.getApplicationId())
                .switchIfEmpty(Mono.error(new NotFoundException("Application", request.getApplicationId())))
                .then(permissionRepository.findByApplicationAndCode(request.getApplicationId(), code))
                .flatMap(existing -> Mono.<Permission>error(new ConflictException(
                        "Permission code already in use in this application: " + code)))
                .switchIfEmpty(Mono.defer(() -> permissionRepository.save(Permission.builder()
                        .applicationId(request.getApplicationId())
                        .code(code)
                        .name(request.getName())
                        .description(request.getDescription())
                        .resource(normalise(request.getResource()))
                        .action(normalise(request.getAction()))
                        .active(true)
                        .createdAt(Instant.now())
                        .createdBy(actorId(actor))
                        .build())))
                .flatMap(saved -> audit(actor, saved.getApplicationId(), "PERMISSION", saved.getId(), saved.getCode())
                        .thenReturn(saved));
    }

    @Override
    public Flux<Permission> listPermissions(Long applicationId) {
        return permissionRepository.findByApplication(applicationId);
    }

    // ── role_permission ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public Mono<RolePermission> grantPermissionToRole(GrantPermissionRequest request, AuthenticatedPrincipal actor) {
        return roleRepository.findById(request.getRoleId())
                .switchIfEmpty(Mono.error(new NotFoundException("Role", request.getRoleId())))
                .flatMap(role -> permissionRepository.findById(request.getPermissionId())
                        .switchIfEmpty(Mono.error(new NotFoundException("Permission", request.getPermissionId())))
                        .flatMap(permission -> {
                            if (!permission.getApplicationId().equals(role.getApplicationId())) {
                                return Mono.<RolePermission>error(new BusinessException(
                                        "Permission " + permission.getId() + " belongs to application "
                                                + permission.getApplicationId() + ", but role "
                                                + role.getId() + " belongs to application "
                                                + role.getApplicationId()));
                            }
                            return rolePermissionRepository
                                    .findByRoleAndPermission(role.getId(), permission.getId())
                                    .flatMap(existing -> Mono.<RolePermission>error(new ConflictException(
                                            "This permission is already granted to the role")))
                                    .switchIfEmpty(Mono.defer(() -> rolePermissionRepository
                                            .save(RolePermission.builder()
                                                    .roleId(role.getId())
                                                    .permissionId(permission.getId())
                                                    .applicationId(role.getApplicationId())
                                                    .createdAt(Instant.now())
                                                    .createdBy(actorId(actor))
                                                    .build())))
                                    .flatMap(saved -> auditRecorder.record(AuditEventType.PERMISSION_GRANTED,
                                                    actorId(actor), role.getApplicationId(), true,
                                                    "Granted permission " + permission.getCode()
                                                            + " to role " + role.getCode(),
                                                    "ROLE_PERMISSION", String.valueOf(saved.getId()), null, null)
                                            .thenReturn(saved));
                        }));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Codes are stored upper case and trimmed so the unique constraints behave predictably. */
    private String normalise(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    private Mono<Void> audit(AuthenticatedPrincipal actor, Long applicationId, String resource,
                             Long resourceId, String code) {
        return auditRecorder.record(AuditEventType.MASTER_DATA_CREATED, actorId(actor), applicationId,
                true, "Created " + resource + " " + code, resource, String.valueOf(resourceId), null, null);
    }

    private Long actorId(AuthenticatedPrincipal actor) {
        return actor == null ? null : actor.getSecurityUserId();
    }
}
