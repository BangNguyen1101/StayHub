package com.quanlynhatro.rental.service;

import com.quanlynhatro.rental.api.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface RentalRequestService {
    RentalRequestResponse submit(
            Authentication authentication, Long roomId, RentalRequestCreateRequest request);

    Page<RentalRequestSummaryResponse> getMyRequests(
            Authentication authentication, RentalRequestStatus status, Pageable pageable);

    RentalRequestResponse getMyRequest(Authentication authentication, Long id);

    RentalRequestResponse updateInformation(
            Authentication authentication, Long id, RentalRequestInfoUpdateRequest request);

    RentalRequestResponse cancel(Authentication authentication, Long id);

    Page<RentalRequestSummaryResponse> searchForAdmin(
            Authentication authentication,
            String keyword,
            RentalRequestStatus status,
            Long applicantUserId,
            Long roomId,
            Pageable pageable);

    RentalRequestResponse getForAdmin(Authentication authentication, Long id);

    RentalRequestResponse decide(
            Authentication authentication, Long id, RentalRequestDecisionRequest decision);

    List<RentalRequestHistoryResponse> getHistoryForAdmin(Authentication authentication, Long id);
}
