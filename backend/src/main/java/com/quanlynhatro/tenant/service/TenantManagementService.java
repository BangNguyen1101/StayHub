package com.quanlynhatro.tenant.service;

import com.quanlynhatro.tenant.api.AdminTenantCreateRequest;
import com.quanlynhatro.tenant.api.AdminTenantProfileUpdateRequest;
import com.quanlynhatro.tenant.api.TenantDetailResponse;
import com.quanlynhatro.tenant.api.TenantSummaryResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TenantManagementService {
    Page<TenantSummaryResponse> search(String keyword, Boolean active, Pageable pageable);

    void deactivate(Long tenantId);

    TenantDetailResponse create(AdminTenantCreateRequest request);

    TenantDetailResponse getById(Long tenantId);

    TenantDetailResponse update(Long tenantId, AdminTenantProfileUpdateRequest request);
}
