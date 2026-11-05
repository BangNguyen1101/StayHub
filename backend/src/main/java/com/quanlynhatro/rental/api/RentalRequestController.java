package com.quanlynhatro.rental.api;

import com.quanlynhatro.rental.service.RentalRequestService;
import com.quanlynhatro.room.api.PageResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RentalRequestController {

    private final RentalRequestService rentalRequestService;

    public RentalRequestController(RentalRequestService rentalRequestService) {
        this.rentalRequestService = rentalRequestService;
    }

    @PostMapping("/rental-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public RentalRequestResponse submit(
            Authentication authentication,
            @RequestParam @Positive Long roomId,
            @Valid @RequestBody RentalRequestCreateRequest request) {
        return rentalRequestService.submit(authentication, roomId, request);
    }

    @GetMapping("/rental-requests/mine")
    public PageResponse<RentalRequestSummaryResponse> mine(
            Authentication authentication,
            @RequestParam(required = false) RentalRequestStatus status,
            Pageable pageable) {
        return PageResponse.from(
                rentalRequestService.getMyRequests(authentication, status, pageable));
    }

    @GetMapping("/rental-requests/{id}")
    public RentalRequestResponse getMine(
            Authentication authentication, @PathVariable @Positive Long id) {
        return rentalRequestService.getMyRequest(authentication, id);
    }

    @PatchMapping("/rental-requests/{id}/cancel")
    public RentalRequestResponse cancel(
            Authentication authentication, @PathVariable @Positive Long id) {
        return rentalRequestService.cancel(authentication, id);
    }

    @PatchMapping("/rental-requests/{id}/supplement")
    public RentalRequestResponse supplement(
            Authentication authentication,
            @PathVariable @Positive Long id,
            @Valid @RequestBody RentalRequestInfoUpdateRequest request) {
        return rentalRequestService.updateInformation(authentication, id, request);
    }

    @GetMapping("/admin/rental-requests")
    public PageResponse<RentalRequestSummaryResponse> searchForAdmin(
            Authentication authentication,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RentalRequestStatus status,
            @RequestParam(required = false) Long applicantUserId,
            @RequestParam(required = false) Long roomId,
            Pageable pageable) {
        return PageResponse.from(
                rentalRequestService.searchForAdmin(
                        authentication, keyword, status, applicantUserId, roomId, pageable));
    }

    @GetMapping("/admin/rental-requests/{id}")
    public RentalRequestResponse getForAdmin(
            Authentication authentication, @PathVariable @Positive Long id) {
        return rentalRequestService.getForAdmin(authentication, id);
    }

    @PatchMapping("/admin/rental-requests/{id}/approve")
    public RentalRequestResponse approve(
            Authentication authentication, @PathVariable @Positive Long id) {
        return rentalRequestService.decide(
                authentication,
                id,
                new RentalRequestDecisionRequest(RentalRequestStatus.APPROVED, null));
    }

    @PatchMapping("/admin/rental-requests/{id}/reject")
    public RentalRequestResponse reject(
            Authentication authentication,
            @PathVariable @Positive Long id,
            @Valid @RequestBody RentalRequestDecisionNoteRequest request) {
        return rentalRequestService.decide(
                authentication,
                id,
                new RentalRequestDecisionRequest(RentalRequestStatus.REJECTED, request.note()));
    }

    @PatchMapping("/admin/rental-requests/{id}/request-info")
    public RentalRequestResponse requestInfo(
            Authentication authentication,
            @PathVariable @Positive Long id,
            @Valid @RequestBody RentalRequestDecisionNoteRequest request) {
        return rentalRequestService.decide(
                authentication,
                id,
                new RentalRequestDecisionRequest(RentalRequestStatus.NEEDS_INFO, request.note()));
    }

    @GetMapping("/admin/rental-requests/{id}/history")
    public List<RentalRequestHistoryResponse> history(
            Authentication authentication, @PathVariable @Positive Long id) {
        return rentalRequestService.getHistoryForAdmin(authentication, id);
    }
}
