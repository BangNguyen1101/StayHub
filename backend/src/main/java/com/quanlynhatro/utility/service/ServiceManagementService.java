package com.quanlynhatro.utility.service;

import com.quanlynhatro.utility.api.FeePricingMode;
import com.quanlynhatro.utility.api.ServiceCreateRequest;
import com.quanlynhatro.utility.api.ServiceResponse;
import com.quanlynhatro.utility.api.ServiceUpdateRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

public interface ServiceManagementService {
    Page<ServiceResponse> search(
            String keyword, FeePricingMode pricingMode, Boolean active, Pageable pageable);

    ServiceResponse create(ServiceCreateRequest request, Authentication authentication);

    ServiceResponse update(Long id, ServiceUpdateRequest request, Authentication authentication);
}
