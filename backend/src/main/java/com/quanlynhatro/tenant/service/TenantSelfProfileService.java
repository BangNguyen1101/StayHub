package com.quanlynhatro.tenant.service;

import com.quanlynhatro.tenant.api.TenantProfileResponse;
import com.quanlynhatro.tenant.api.TenantSelfProfileUpdateRequest;

public interface TenantSelfProfileService {
    TenantProfileResponse getMyProfile(String authenticatedUsername);

    TenantProfileResponse updateMyProfile(
            String authenticatedUsername, TenantSelfProfileUpdateRequest request);
}
