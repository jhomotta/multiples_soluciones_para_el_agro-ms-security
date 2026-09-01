package com.msagro.security.usecase;

import com.msagro.security.model.admin.AssignRoleRequest;
import com.msagro.security.model.admin.GrantAccessRequest;
import com.msagro.security.model.admin.UserAccessView;
import com.msagro.security.model.auth.AuthenticatedPrincipal;
import com.msagro.security.model.auth.ClientContext;
import com.msagro.security.model.enums.AuditEventType;
import com.msagro.security.model.error.BusinessException;
import com.msagro.security.model.error.ConflictException;
import com.msagro.security.model.error.NotFoundException;
import com.msagro.security.model.role.Role;
import com.msagro.security.model.userapplication.UserApplication;
import com.msagro.security.model.userrole.UserRole;
import com.msagro.security.usecase.business.AccessManagementUseCase;
import com.msagro.security.usecase.gateway.securitydb.ApplicationRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.PersonRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.RoleRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.SecurityUserRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.UserApplicationRepositoryPort;
import com.msagro.security.usecase.gateway.securitydb.UserRoleRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * Administration of access grants and role assignments.
 *
 * <p>The database already refuses to attach a role from another application, thanks to the
 * composite foreign keys on {@code user_role}. This use case checks the same rule up front so the
 * caller gets a clear 400 instead of a constraint violation, and so the failure is auditable.</p>
 *
 * <p>Every change to what a user may do bumps {@code security_stamp}: already-issued access
 * tokens still carry the old authority list, and the stamp is what lets the next refresh notice.</p>
 */
@Service
public class AccessManagementUseCaseImpl implements AccessManagementUseCase {

    private final UserApplicationRepositoryPort userApplicationRepository;
    private final UserRoleRepositoryPort userRoleRepository;
    private final SecurityUserRepositoryPort userRepository;
    private final PersonRepositoryPort personRepository;
    private final RoleRepositoryPort roleRepository;
    private final ApplicationRepositoryPort applicationRepository;
    private final AuditRecorder auditRecorder;

    public AccessManagementUseCaseImpl(UserApplicationRepositoryPort userApplicationRepository,
                                       UserRoleRepositoryPort userRoleRepository,
                                       SecurityUserRepositoryPort userRepository,
                                       PersonRepositoryPort personRepository,
                                       RoleRepositoryPort roleRepository,
                                       ApplicationRepositoryPort applicationRepository,
                                       AuditRecorder auditRecorder) {
        this.userApplicationRepository = userApplicationRepository;
        this.userRoleRepository = userRoleRepository;
        this.userRepository = userRepository;
        this.personRepository = personRepository;
        this.roleRepository = roleRepository;
        this.applicationRepository = applicationRepository;
        this.auditRecorder = auditRecorder;
    }

    @Override
    @Transactional
    public Mono<UserApplication> grantAccess(GrantAccessRequest request,
                                             AuthenticatedPrincipal actor,
                                             ClientContext context) {
        if (request.getAccessStartAt() != null && request.getAccessEndAt() != null
                && !request.getAccessEndAt().isAfter(request.getAccessStartAt())) {
            return Mono.error(new BusinessException("accessEndAt must be after accessStartAt"));
        }

        return userRepository.findById(request.getSecurityUserId())
                .switchIfEmpty(Mono.error(new NotFoundException("SecurityUser", request.getSecurityUserId())))
                .then(applicationRepository.findById(request.getApplicationId()))
                .switchIfEmpty(Mono.error(new NotFoundException("Application", request.getApplicationId())))
                .then(userApplicationRepository
                        .findByUserAndApplication(request.getSecurityUserId(), request.getApplicationId()))
                .flatMap(existing -> Mono.<UserApplication>error(new ConflictException(
                        "This user already has access to application " + request.getApplicationId())))
                .switchIfEmpty(Mono.defer(() -> userApplicationRepository.save(UserApplication.builder()
                        .securityUserId(request.getSecurityUserId())
                        .applicationId(request.getApplicationId())
                        .active(true)
                        .accessStartAt(request.getAccessStartAt())
                        .accessEndAt(request.getAccessEndAt())
                        .createdAt(Instant.now())
                        .createdBy(actorId(actor))
                        .build())))
                .flatMap(saved -> auditRecorder.record(AuditEventType.ACCESS_GRANTED, actorId(actor),
                                request.getApplicationId(), true,
                                "Granted user " + request.getSecurityUserId() + " access to application "
                                        + request.getApplicationId(),
                                "USER_APPLICATION", String.valueOf(saved.getId()),
                                metadata("securityUserId", saved.getSecurityUserId(),
                                        "applicationId", saved.getApplicationId()),
                                context)
                        .thenReturn(saved));
    }

    @Override
    @Transactional
    public Mono<Void> revokeAccess(Long userApplicationId,
                                   AuthenticatedPrincipal actor,
                                   ClientContext context) {
        return userApplicationRepository.findById(userApplicationId)
                .switchIfEmpty(Mono.error(new NotFoundException("UserApplication", userApplicationId)))
                .flatMap(grant -> userApplicationRepository.deactivate(grant.getId())
                        .then(userRepository.bumpSecurityStamp(grant.getSecurityUserId()))
                        .then(auditRecorder.record(AuditEventType.ACCESS_REVOKED, actorId(actor),
                                grant.getApplicationId(), true,
                                "Revoked access grant " + userApplicationId,
                                "USER_APPLICATION", String.valueOf(userApplicationId), null, context)));
    }

    @Override
    @Transactional
    public Mono<UserRole> assignRole(AssignRoleRequest request,
                                     AuthenticatedPrincipal actor,
                                     ClientContext context) {
        return userApplicationRepository.findById(request.getUserApplicationId())
                .switchIfEmpty(Mono.error(new NotFoundException("UserApplication", request.getUserApplicationId())))
                .flatMap(grant -> roleRepository.findById(request.getRoleId())
                        .switchIfEmpty(Mono.error(new NotFoundException("Role", request.getRoleId())))
                        .flatMap(role -> attach(grant, role, request, actor, context)));
    }

    @Override
    @Transactional
    public Mono<Void> revokeRole(Long userApplicationId,
                                 Long roleId,
                                 AuthenticatedPrincipal actor,
                                 ClientContext context) {
        return userRoleRepository.findByGrantAndRole(userApplicationId, roleId)
                .switchIfEmpty(Mono.error(new NotFoundException(
                        "Role " + roleId + " is not assigned to access grant " + userApplicationId)))
                .flatMap(assignment -> userRoleRepository.deactivate(userApplicationId, roleId)
                        .then(userApplicationRepository.findById(userApplicationId))
                        .flatMap(grant -> userRepository.bumpSecurityStamp(grant.getSecurityUserId())
                                .then(auditRecorder.record(AuditEventType.ROLE_REVOKED, actorId(actor),
                                        grant.getApplicationId(), true,
                                        "Revoked role " + roleId + " from access grant " + userApplicationId,
                                        "USER_ROLE", String.valueOf(assignment.getId()), null, context))));
    }

    @Override
    public Mono<UserAccessView> describeAccess(Long userApplicationId) {
        return userApplicationRepository.findById(userApplicationId)
                .switchIfEmpty(Mono.error(new NotFoundException("UserApplication", userApplicationId)))
                .flatMap(this::toView);
    }

    @Override
    public Flux<UserAccessView> listAccessOfUser(Long securityUserId) {
        return userApplicationRepository.findByUser(securityUserId).flatMap(this::toView);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Mono<UserRole> attach(UserApplication grant,
                                  Role role,
                                  AssignRoleRequest request,
                                  AuthenticatedPrincipal actor,
                                  ClientContext context) {
        if (!role.getApplicationId().equals(grant.getApplicationId())) {
            return Mono.error(new BusinessException(
                    "Role " + role.getId() + " belongs to application " + role.getApplicationId()
                            + ", but the access grant is for application " + grant.getApplicationId()));
        }
        if (request.getValidFrom() != null && request.getValidUntil() != null
                && !request.getValidUntil().isAfter(request.getValidFrom())) {
            return Mono.error(new BusinessException("validUntil must be after validFrom"));
        }

        return userRoleRepository.findByGrantAndRole(grant.getId(), role.getId())
                .flatMap(existing -> Mono.<UserRole>error(new ConflictException(
                        "Role " + role.getId() + " is already assigned to this access grant")))
                .switchIfEmpty(Mono.defer(() -> userRoleRepository.save(UserRole.builder()
                        .userApplicationId(grant.getId())
                        .roleId(role.getId())
                        .applicationId(grant.getApplicationId())
                        .active(true)
                        .validFrom(request.getValidFrom())
                        .validUntil(request.getValidUntil())
                        .createdAt(Instant.now())
                        .createdBy(actorId(actor))
                        .build())))
                .flatMap(saved -> userRepository.bumpSecurityStamp(grant.getSecurityUserId())
                        .then(auditRecorder.record(AuditEventType.ROLE_ASSIGNED, actorId(actor),
                                grant.getApplicationId(), true,
                                "Assigned role " + role.getCode() + " to access grant " + grant.getId(),
                                "USER_ROLE", String.valueOf(saved.getId()),
                                metadata("roleCode", role.getCode(),
                                        "userApplicationId", grant.getId()),
                                context))
                        .thenReturn(saved));
    }

    private Mono<UserAccessView> toView(UserApplication grant) {
        Mono<List<String>> roles = userRoleRepository.findRoleCodes(grant.getId()).collectList();
        Mono<List<String>> authorities = userRoleRepository.findAuthorities(grant.getId()).collectList();

        return Mono.zip(roles, authorities)
                .flatMap(tuple -> userRepository.findById(grant.getSecurityUserId())
                        .flatMap(user -> personRepository.findById(user.getPersonId())
                                .map(person -> UserAccessView.builder()
                                        .userApplicationId(grant.getId())
                                        .securityUserId(user.getId())
                                        .username(user.getUsername())
                                        .fullName(person.fullName())
                                        .email(person.getEmail())
                                        .applicationId(grant.getApplicationId())
                                        .active(grant.getActive())
                                        .accessStartAt(grant.getAccessStartAt())
                                        .accessEndAt(grant.getAccessEndAt())
                                        .roles(tuple.getT1())
                                        .permissions(tuple.getT2().stream()
                                                .filter(authority -> !authority.startsWith("ROLE_"))
                                                .toList())
                                        .build())))
                .flatMap(view -> applicationRepository.findById(grant.getApplicationId())
                        .map(application -> {
                            view.setApplicationCode(application.getCode());
                            return view;
                        })
                        .defaultIfEmpty(view));
    }

    private Long actorId(AuthenticatedPrincipal actor) {
        return actor == null ? null : actor.getSecurityUserId();
    }

    /**
     * Builds the small JSON document stored in {@code security_audit.metadata} (a {@code jsonb}
     * column). Keys are fixed literals and values are ids or codes the service produced, so this
     * stays a two-key builder rather than a general serialiser — and nothing user-supplied is
     * concatenated into JSON here.
     */
    private String metadata(String firstKey, Object firstValue, String secondKey, Object secondValue) {
        return "{\"" + firstKey + "\":" + jsonValue(firstValue)
                + ",\"" + secondKey + "\":" + jsonValue(secondValue) + "}";
    }

    private String jsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        return value instanceof Number ? value.toString() : "\"" + value + "\"";
    }
}
