package com.quanlynhatro.tenant.service;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.domain.TenantProfileEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.auth.repository.TenantProfileRepository;
import com.quanlynhatro.tenant.api.TenantProfileResponse;
import com.quanlynhatro.tenant.api.TenantSelfProfileUpdateRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TenantSelfProfileServiceImpl implements TenantSelfProfileService {
    private static final String TENANT_ROLE = "TENANT";

    private final AppUserRepository users;
    private final TenantProfileRepository profiles;

    public TenantSelfProfileServiceImpl(AppUserRepository users, TenantProfileRepository profiles) {
        this.users = users;
        this.profiles = profiles;
    }

    @Override
    public TenantProfileResponse getMyProfile(String authenticatedUsername) {
        return toResponse(findTenant(authenticatedUsername), true);
    }

    @Override
    @Transactional
    public TenantProfileResponse updateMyProfile(
            String authenticatedUsername, TenantSelfProfileUpdateRequest request) {
        AppUserEntity user = findTenant(authenticatedUsername);
        TenantProfileEntity profile = findProfile(user.getId());
        user.updateTenantProfile(request.fullName().trim(), normalize(request.phoneNumber()));
        profile.updateContactAddress(normalize(request.contactAddress()));
        return toResponse(user, true);
    }

    private AppUserEntity findTenant(String authenticatedUsername) {
        return users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                        authenticatedUsername, authenticatedUsername)
                .filter(
                        user ->
                                user.getRole() != null
                                        && TENANT_ROLE.equals(user.getRole().getCode()))
                .orElseThrow(() -> new TenantNotFoundException(authenticatedUsername));
    }

    private TenantProfileEntity findProfile(Long tenantId) {
        return profiles.findByUserIdAndUser_Role_Code(tenantId, TENANT_ROLE)
                .orElseThrow(() -> new TenantProfileNotFoundException(tenantId));
    }

    private TenantProfileResponse toResponse(AppUserEntity user, boolean includeProfile) {
        String contactAddress =
                includeProfile ? findProfile(user.getId()).getContactAddress() : null;
        return new TenantProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                contactAddress,
                user.isActive());
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
