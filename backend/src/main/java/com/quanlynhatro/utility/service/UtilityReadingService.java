package com.quanlynhatro.utility.service;

import com.quanlynhatro.utility.api.UtilityReadingCreateRequest;
import com.quanlynhatro.utility.api.UtilityReadingLockRequest;
import com.quanlynhatro.utility.api.UtilityReadingResponse;
import com.quanlynhatro.utility.api.UtilityReadingUpdateRequest;
import com.quanlynhatro.utility.api.UtilityType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

public interface UtilityReadingService {
    Page<UtilityReadingResponse> search(
            Long roomId, UtilityType utilityType, Boolean locked, Pageable pageable);

    UtilityReadingResponse getById(Long id);

    UtilityReadingResponse create(
            UtilityReadingCreateRequest request, Authentication authentication);

    UtilityReadingResponse update(
            Long id, UtilityReadingUpdateRequest request, Authentication authentication);

    UtilityReadingResponse lock(
            Long id, UtilityReadingLockRequest request, Authentication authentication);
}
