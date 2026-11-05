package com.quanlynhatro.utility.service;

import com.quanlynhatro.utility.api.FeeConfigCreateRequest;
import com.quanlynhatro.utility.api.FeeConfigResponse;
import com.quanlynhatro.utility.api.FeeConfigUpdateRequest;
import com.quanlynhatro.utility.api.FeePricingMode;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;

public interface FeeConfigService {
    Page<FeeConfigResponse> search(
            String keyword, FeePricingMode pricingMode, Boolean active, Pageable pageable);

    FeeConfigResponse create(FeeConfigCreateRequest request, Authentication authentication);

    FeeConfigResponse update(
            Long id, FeeConfigUpdateRequest request, Authentication authentication);

    FeeConfigResponse findEffective(String feeCode, LocalDate atDate);
}
