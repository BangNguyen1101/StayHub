package com.quanlynhatro.tenant.api;

import com.quanlynhatro.room.api.PageResponse;
import com.quanlynhatro.tenant.service.TenantManagementService;
import com.quanlynhatro.tenant.service.TenantSelfProfileService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TenantController {
    private final TenantManagementService tenantManagementService;
    private final TenantSelfProfileService tenantSelfProfileService;

    public TenantController(
            TenantManagementService tenantManagementService,
            TenantSelfProfileService tenantSelfProfileService) {
        this.tenantManagementService = tenantManagementService;
        this.tenantSelfProfileService = tenantSelfProfileService;
    }

    @GetMapping("/admin/tenants")
    public PageResponse<TenantSummaryResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return PageResponse.from(tenantManagementService.search(keyword, active, pageable));
    }

    @org.springframework.web.bind.annotation.PostMapping("/admin/tenants")
    @org.springframework.web.bind.annotation.ResponseStatus(
            org.springframework.http.HttpStatus.CREATED)
    public TenantDetailResponse create(@Valid @RequestBody AdminTenantCreateRequest request) {
        return tenantManagementService.create(request);
    }

    @GetMapping("/admin/tenants/{userId}")
    public TenantDetailResponse getById(@PathVariable @Positive Long userId) {
        return tenantManagementService.getById(userId);
    }

    @PatchMapping("/admin/tenants/{userId}/profile")
    public TenantDetailResponse update(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody AdminTenantProfileUpdateRequest request) {
        return tenantManagementService.update(userId, request);
    }

    @DeleteMapping("/admin/tenants/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable @Positive Long userId) {
        tenantManagementService.deactivate(userId);
    }

    @GetMapping("/tenant/profile")
    public TenantProfileResponse getMyProfile(Authentication authentication) {
        return tenantSelfProfileService.getMyProfile(authentication.getName());
    }

    @PatchMapping("/tenant/profile")
    public TenantProfileResponse updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody TenantSelfProfileUpdateRequest request) {
        return tenantSelfProfileService.updateMyProfile(authentication.getName(), request);
    }
}
