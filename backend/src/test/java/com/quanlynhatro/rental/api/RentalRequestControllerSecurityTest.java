package com.quanlynhatro.rental.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quanlynhatro.auth.api.AuthExceptionHandler;
import com.quanlynhatro.auth.security.AppUserDetailsService;
import com.quanlynhatro.auth.security.JwtService;
import com.quanlynhatro.auth.security.SecurityConfig;
import com.quanlynhatro.rental.service.RentalRequestAccessDeniedException;
import com.quanlynhatro.rental.service.RentalRequestBusinessException;
import com.quanlynhatro.rental.service.RentalRequestService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

@WebMvcTest(RentalRequestController.class)
@Import({SecurityConfig.class, AuthExceptionHandler.class})
class RentalRequestControllerSecurityTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RentalRequestService rentalRequestService;

    @MockitoBean private AppUserDetailsService userDetailsService;

    @MockitoBean private JwtService jwtService;

    @Test
    void unauthenticatedCannotAccessTenantRentalApis() throws Exception {
        mockMvc.perform(get("/api/rental-requests/mine"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(
                        post("/api/rental-requests")
                                .param("roomId", "1")
                                .contentType("application/json")
                                .content(validCreateJson()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/rental-requests/1/cancel"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedCannotAccessAdminRentalApis() throws Exception {
        mockMvc.perform(get("/api/admin/rental-requests")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/admin/rental-requests/1/approve"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tenantCannotAccessAdminRentalApis() throws Exception {
        mockMvc.perform(get("/api/admin/rental-requests").with(user("tenant").roles("TENANT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(
                        patch("/api/admin/rental-requests/1/approve")
                                .with(user("tenant").roles("TENANT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotUseTenantRentalApis() throws Exception {
        mockMvc.perform(get("/api/rental-requests/mine").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void tenantCanSubmitRentalRequest() throws Exception {
        when(rentalRequestService.submit(any(), eq(5L), any(RentalRequestCreateRequest.class)))
                .thenReturn(response());

        mockMvc.perform(
                        post("/api/rental-requests")
                                .param("roomId", "5")
                                .with(user("tenant").roles("TENANT"))
                                .contentType("application/json")
                                .content(validCreateJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestCode").value("RR-20261019-ABC12345"));
    }

    @Test
    void invalidSubmitRequestReturnsUnifiedValidationError() throws Exception {
        mockMvc.perform(
                        post("/api/rental-requests")
                                .param("roomId", "5")
                                .with(user("tenant").roles("TENANT"))
                                .contentType("application/json")
                                .content("{\"contactName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verify(rentalRequestService, never()).submit(any(), any(), any());
    }

    @Test
    void tenantCanListOwnRequestsWithPagination() throws Exception {
        when(rentalRequestService.getMyRequests(any(), eq(null), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(summary())));

        mockMvc.perform(
                        get("/api/rental-requests/mine")
                                .param("page", "0")
                                .param("size", "10")
                                .with(user("tenant").roles("TENANT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].requestCode").value("RR-20261019-ABC12345"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void tenantCannotReadAnotherTenantsRequest() throws Exception {
        when(rentalRequestService.getMyRequest(any(), eq(99L)))
                .thenThrow(new RentalRequestAccessDeniedException());

        mockMvc.perform(get("/api/rental-requests/99").with(user("tenant").roles("TENANT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RENTAL_REQUEST_ACCESS_DENIED"));
    }

    @Test
    void tenantCanCancelAndSupplementRequest() throws Exception {
        when(rentalRequestService.cancel(any(), eq(1L))).thenReturn(response());
        when(rentalRequestService.updateInformation(
                        any(), eq(1L), any(RentalRequestInfoUpdateRequest.class)))
                .thenReturn(response());

        mockMvc.perform(patch("/api/rental-requests/1/cancel").with(user("tenant").roles("TENANT")))
                .andExpect(status().isOk());
        mockMvc.perform(
                        patch("/api/rental-requests/1/supplement")
                                .with(user("tenant").roles("TENANT"))
                                .contentType("application/json")
                                .content(validCreateJson()))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanApproveRequest() throws Exception {
        when(rentalRequestService.decide(any(), eq(1L), any(RentalRequestDecisionRequest.class)))
                .thenReturn(response());

        mockMvc.perform(
                        patch("/api/admin/rental-requests/1/approve")
                                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(rentalRequestService)
                .decide(
                        any(),
                        eq(1L),
                        eq(new RentalRequestDecisionRequest(RentalRequestStatus.APPROVED, null)));
    }

    @Test
    void rejectRequiresProcessingNote() throws Exception {
        mockMvc.perform(
                        patch("/api/admin/rental-requests/1/reject")
                                .with(user("admin").roles("ADMIN"))
                                .contentType("application/json")
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verify(rentalRequestService, never()).decide(any(), any(), any());
    }

    @Test
    void adminCanRejectWithNoteAndCannotOverridePathDecision() throws Exception {
        when(rentalRequestService.decide(any(), eq(1L), any(RentalRequestDecisionRequest.class)))
                .thenReturn(response());

        mockMvc.perform(
                        patch("/api/admin/rental-requests/1/reject")
                                .with(user("admin").roles("ADMIN"))
                                .contentType("application/json")
                                .content("{\"note\":\"Thiếu giấy tờ\"}"))
                .andExpect(status().isOk());
        verify(rentalRequestService)
                .decide(
                        any(),
                        eq(1L),
                        eq(
                                new RentalRequestDecisionRequest(
                                        RentalRequestStatus.REJECTED, "Thiếu giấy tờ")));
    }

    @Test
    void adminCanSearchAndRequestInformation() throws Exception {
        when(rentalRequestService.searchForAdmin(
                        any(),
                        eq("P01"),
                        eq(RentalRequestStatus.PENDING),
                        eq(10L),
                        eq(5L),
                        any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(summary())));
        when(rentalRequestService.decide(any(), eq(1L), any(RentalRequestDecisionRequest.class)))
                .thenReturn(response());

        mockMvc.perform(
                        get("/api/admin/rental-requests")
                                .param("keyword", "P01")
                                .param("status", "PENDING")
                                .param("applicantUserId", "10")
                                .param("roomId", "5")
                                .param("page", "0")
                                .param("size", "10")
                                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].roomCode").value("P01"));
        mockMvc.perform(
                        patch("/api/admin/rental-requests/1/request-info")
                                .with(user("admin").roles("ADMIN"))
                                .contentType("application/json")
                                .content("{\"note\":\"Bổ sung CCCD\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanReadHistory() throws Exception {
        when(rentalRequestService.getHistoryForAdmin(any(), eq(1L))).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/admin/rental-requests/1/history")
                                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").doesNotExist());
    }

    @Test
    void businessErrorUsesUnifiedConflictResponse() throws Exception {
        when(rentalRequestService.cancel(any(), eq(1L)))
                .thenThrow(
                        new RentalRequestBusinessException("Only open requests can be cancelled"));

        mockMvc.perform(patch("/api/rental-requests/1/cancel").with(user("tenant").roles("TENANT")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RENTAL_REQUEST_CONFLICT"));
    }

    private RentalRequestResponse response() {
        return new RentalRequestResponse(
                1L,
                "RR-20261019-ABC12345",
                10L,
                5L,
                "P01",
                "Tenant",
                "0900000000",
                "tenant@example.com",
                1,
                LocalDate.of(2026, 11, 1),
                null,
                RentalRequestStatus.PENDING,
                null,
                null,
                null,
                null,
                null);
    }

    private RentalRequestSummaryResponse summary() {
        return new RentalRequestSummaryResponse(
                1L,
                "RR-20261019-ABC12345",
                5L,
                "P01",
                "Tenant",
                1,
                LocalDate.of(2026, 11, 1),
                RentalRequestStatus.PENDING,
                null);
    }

    private String validCreateJson() {
        return """
               {
                 "contactName":"Tenant",
                 "contactPhone":"0900000000",
                 "contactEmail":"tenant@example.com",
                 "expectedOccupantCount":1,
                 "expectedMoveInDate":"2026-11-01",
                 "note":""
               }
               """;
    }
}
