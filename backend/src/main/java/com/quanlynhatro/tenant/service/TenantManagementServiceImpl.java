package com.quanlynhatro.tenant.service;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.domain.TenantProfileEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.auth.repository.RoleRepository;
import com.quanlynhatro.auth.repository.TenantProfileRepository;
import com.quanlynhatro.contract.api.ContractStatus;
import com.quanlynhatro.contract.repository.ContractRepository;
import com.quanlynhatro.tenant.api.AdminTenantCreateRequest;
import com.quanlynhatro.tenant.api.AdminTenantProfileUpdateRequest;
import com.quanlynhatro.tenant.api.TenantDetailResponse;
import com.quanlynhatro.tenant.api.TenantSummaryResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TenantManagementServiceImpl implements TenantManagementService {
    private static final String TENANT_ROLE = "TENANT";

    private final AppUserRepository users;
    private final TenantProfileRepository profiles;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final ContractRepository contracts;

    public TenantManagementServiceImpl(
            AppUserRepository users,
            TenantProfileRepository profiles,
            RoleRepository roles,
            PasswordEncoder passwordEncoder) {
        this(users, profiles, roles, passwordEncoder, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public TenantManagementServiceImpl(
            AppUserRepository users,
            TenantProfileRepository profiles,
            RoleRepository roles,
            PasswordEncoder passwordEncoder,
            ContractRepository contracts) {
        this.users = users;
        this.profiles = profiles;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.contracts = contracts;
    }

    @Override
    public Page<TenantSummaryResponse> search(String keyword, Boolean active, Pageable pageable) {
        return users.searchTenants(normalize(keyword), active, pageable).map(this::toSummary);
    }

    @Override
    @Transactional
    public void deactivate(Long tenantId) {
        AppUserEntity user = findTenant(tenantId);
        if (contracts != null
                && contracts.existsByTenantUser_IdAndStatusIn(
                        tenantId,
                        java.util.Set.of(ContractStatus.ACTIVE, ContractStatus.EXPIRING))) {
            throw new TenantDeletionConflictException();
        }
        user.deactivate();
    }

    @Override
    @Transactional
    public TenantDetailResponse create(AdminTenantCreateRequest request) {
        String username = normalizeRequired(request.username());
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new TenantAccountConflictException();
        }
        String email = internalEmail(username);
        var role =
                roles.findByCode(TENANT_ROLE)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "TENANT role is missing from database"));
        AppUserEntity user =
                users.save(
                        AppUserEntity.createTenant(
                                username,
                                email,
                                passwordEncoder.encode(request.password()),
                                role,
                                normalizeRequired(request.fullName()),
                                normalize(request.phoneNumber())));
        TenantProfileEntity profile = TenantProfileEntity.createFor(user);
        profiles.save(profile);
        return toDetail(user);
    }

    @Override
    public TenantDetailResponse getById(Long tenantId) {
        return toDetail(findTenant(tenantId));
    }

    @Override
    @Transactional
    public TenantDetailResponse update(Long tenantId, AdminTenantProfileUpdateRequest request) {
        AppUserEntity user = findTenant(tenantId);
        String email = normalizeRequired(request.email());
        if (!email.equalsIgnoreCase(user.getEmail())
                && users.existsByEmailIgnoreCaseAndIdNot(email, tenantId)) {
            throw new DuplicateTenantEmailException(email);
        }

        user.updateAdminProfile(
                email, normalizeRequired(request.fullName()), normalize(request.phoneNumber()));
        TenantProfileEntity profile = findProfile(tenantId);
        profile.updateAdminFields(
                normalize(request.contactAddress()),
                normalize(request.identityDocumentType()),
                normalize(request.identityDocumentNumber()),
                request.identityIssuedDate(),
                normalize(request.identityIssuedPlace()),
                normalize(request.notes()));
        return toDetail(user);
    }

    private AppUserEntity findTenant(Long tenantId) {
        return users.findByIdAndRole_Code(tenantId, TENANT_ROLE)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
    }

    private TenantProfileEntity findProfile(Long tenantId) {
        return profiles.findByUserIdAndUser_Role_Code(tenantId, TENANT_ROLE)
                .orElseThrow(() -> new TenantProfileNotFoundException(tenantId));
    }

    private TenantSummaryResponse toSummary(AppUserEntity user) {
        String contactAddress =
                profiles.findByUserIdAndUser_Role_Code(user.getId(), TENANT_ROLE)
                        .map(TenantProfileEntity::getContactAddress)
                        .orElse(null);
        return new TenantSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                contactAddress,
                user.isActive());
    }

    private TenantDetailResponse toDetail(AppUserEntity user) {
        TenantProfileEntity profile = findProfile(user.getId());
        return new TenantDetailResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.isActive(),
                profile.getContactAddress(),
                profile.getIdentityDocumentType(),
                profile.getIdentityDocumentNumber(),
                profile.getIdentityIssuedDate(),
                profile.getIdentityIssuedPlace(),
                profile.getNotes(),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeRequired(String value) {
        return normalize(value);
    }

    private String internalEmail(String username) {
        String localPart = username.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "-");
        return localPart + "." + UUID.randomUUID() + "@no-email.local";
    }
}
