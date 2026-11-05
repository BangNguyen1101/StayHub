package com.quanlynhatro.utility.api;

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
import com.quanlynhatro.utility.service.DuplicateServiceCodeException;
import com.quanlynhatro.utility.service.FeeConfigPeriodOverlapException;
import com.quanlynhatro.utility.service.FeeConfigService;
import com.quanlynhatro.utility.service.ServiceManagementService;
import com.quanlynhatro.utility.service.UtilityReadingService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@WebMvcTest(UtilityAdminController.class)
@Import({SecurityConfig.class, AuthExceptionHandler.class})
class UtilityAdminControllerSecurityTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean FeeConfigService feeConfigs;
    @MockitoBean ServiceManagementService services;
    @MockitoBean UtilityReadingService readings;
    @MockitoBean AppUserDetailsService userDetailsService;
    @MockitoBean JwtService jwtService;

    @Test
    void unauthenticatedCannotAccessUtilityAdminApis() throws Exception {
        mockMvc.perform(get("/api/admin/fee-configs")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/services")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/utility-readings")).andExpect(status().isUnauthorized());
    }

    @Test
    void tenantCannotAccessUtilityAdminApis() throws Exception {
        var tenant = user("tenant01").roles("TENANT");
        mockMvc.perform(get("/api/admin/fee-configs").with(tenant))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        post("/api/admin/services")
                                .with(tenant)
                                .contentType("application/json")
                                .content(validServiceJson()))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        patch("/api/admin/utility-readings/1/lock")
                                .with(tenant)
                                .contentType("application/json")
                                .content("{\"locked\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanSearchFeeConfigsWithPageResponse() throws Exception {
        when(feeConfigs.search(eq("electric"), eq(FeePricingMode.PER_USAGE), eq(true), any()))
                .thenReturn(new PageImpl<>(List.of(feeResponse())));

        mockMvc.perform(
                        get("/api/admin/fee-configs")
                                .param("keyword", "electric")
                                .param("pricingMode", "PER_USAGE")
                                .param("active", "true")
                                .param("page", "0")
                                .param("size", "10")
                                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].feeCode").value("ELEC"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void adminCanCreateAndUpdateFeeConfig() throws Exception {
        when(feeConfigs.create(any(FeeConfigCreateRequest.class), any())).thenReturn(feeResponse());
        when(feeConfigs.update(eq(1L), any(FeeConfigUpdateRequest.class), any()))
                .thenReturn(feeResponse());

        mockMvc.perform(
                        post("/api/admin/fee-configs")
                                .with(user("admin").roles("ADMIN"))
                                .contentType("application/json")
                                .content(validFeeJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.feeCode").value("ELEC"));
        mockMvc.perform(
                        patch("/api/admin/fee-configs/1")
                                .with(user("admin").roles("ADMIN"))
                                .contentType("application/json")
                                .content(validFeeUpdateJson()))
                .andExpect(status().isOk());
    }

    @Test
    void invalidFeeRequestDoesNotCallService() throws Exception {
        mockMvc.perform(
                        post("/api/admin/fee-configs")
                                .with(user("admin").roles("ADMIN"))
                                .contentType("application/json")
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verify(feeConfigs, never()).create(any(), any());
    }

    @Test
    void feeOverlapIsMappedToConflict() throws Exception {
        when(feeConfigs.create(any(FeeConfigCreateRequest.class), any()))
                .thenThrow(new FeeConfigPeriodOverlapException("ELEC"));

        mockMvc.perform(
                        post("/api/admin/fee-configs")
                                .with(user("admin").roles("ADMIN"))
                                .contentType("application/json")
                                .content(validFeeJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("UTILITY_CONFLICT"));
    }

    @Test
    void adminCanManageServicesAndDuplicateCodeIsConflict() throws Exception {
        when(services.create(any(ServiceCreateRequest.class), any())).thenReturn(serviceResponse());
        mockMvc.perform(
                        post("/api/admin/services")
                                .with(user("admin").roles("ADMIN"))
                                .contentType("application/json")
                                .content(validServiceJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceCode").value("WIFI"));

        when(services.create(any(ServiceCreateRequest.class), any()))
                .thenThrow(new DuplicateServiceCodeException("WIFI"));
        mockMvc.perform(
                        post("/api/admin/services")
                                .with(user("admin").roles("ADMIN"))
                                .contentType("application/json")
                                .content(validServiceJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("UTILITY_CONFLICT"));
    }

    @Test
    void adminCanSearchAndCreateUtilityReading() throws Exception {
        when(readings.search(eq(5L), eq(UtilityType.ELECTRICITY), eq(false), any()))
                .thenReturn(new PageImpl<>(List.of(readingResponse())));
        when(readings.create(any(UtilityReadingCreateRequest.class), any()))
                .thenReturn(readingResponse());

        mockMvc.perform(
                        get("/api/admin/utility-readings")
                                .param("roomId", "5")
                                .param("utilityType", "ELECTRICITY")
                                .param("locked", "false")
                                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].consumption").value(12.5));
        mockMvc.perform(
                        post("/api/admin/utility-readings")
                                .with(user("admin").roles("ADMIN"))
                                .contentType("application/json")
                                .content(validReadingJson()))
                .andExpect(status().isCreated());
    }

    @Test
    void adminCanUpdateAndLockReading() throws Exception {
        when(readings.update(eq(1L), any(UtilityReadingUpdateRequest.class), any()))
                .thenReturn(readingResponse());
        when(readings.lock(eq(1L), any(UtilityReadingLockRequest.class), any()))
                .thenReturn(readingResponse());
        mockMvc.perform(
                        patch("/api/admin/utility-readings/1")
                                .with(user("admin").roles("ADMIN"))
                                .contentType("application/json")
                                .content(validReadingUpdateJson()))
                .andExpect(status().isOk());
        mockMvc.perform(
                        patch("/api/admin/utility-readings/1/lock")
                                .with(user("admin").roles("ADMIN"))
                                .contentType("application/json")
                                .content("{\"locked\":true}"))
                .andExpect(status().isOk());
    }

    private FeeConfigResponse feeResponse() {
        return new FeeConfigResponse(
                1L,
                "ELEC",
                "Electricity",
                "kWh",
                new BigDecimal("3500.00"),
                FeePricingMode.PER_USAGE,
                LocalDate.of(2026, 1, 1),
                null,
                true,
                null);
    }

    private ServiceResponse serviceResponse() {
        return new ServiceResponse(
                1L,
                "WIFI",
                "WiFi",
                FeePricingMode.PER_ROOM,
                new BigDecimal("100000.00"),
                true,
                null);
    }

    private UtilityReadingResponse readingResponse() {
        return new UtilityReadingResponse(
                1L,
                5L,
                UtilityType.ELECTRICITY,
                "2026-01",
                BigDecimal.ZERO,
                new BigDecimal("12.500"),
                new BigDecimal("12.500"),
                LocalDate.of(2026, 1, 31),
                false,
                null,
                null,
                null);
    }

    private String validFeeJson() {
        return """
{"feeCode":"ELEC","feeName":"Electricity","unitName":"kWh","unitPrice":3500,"pricingMode":"PER_USAGE","effectiveFrom":"2026-01-01","active":true}
""";
    }

    private String validFeeUpdateJson() {
        return """
{"feeName":"Electricity","unitName":"kWh","unitPrice":3500,"pricingMode":"PER_USAGE","effectiveFrom":"2026-01-01","active":true}
""";
    }

    private String validServiceJson() {
        return """
{"serviceCode":"WIFI","serviceName":"WiFi","pricingMode":"PER_ROOM","defaultUnitPrice":100000,"active":true}
""";
    }

    private String validReadingJson() {
        return """
{"roomId":5,"utilityType":"ELECTRICITY","billingPeriod":"2026-01","readingStart":0,"readingEnd":12.5,"readingDate":"2026-01-31"}
""";
    }

    private String validReadingUpdateJson() {
        return """
               {"readingStart":0,"readingEnd":12.5,"readingDate":"2026-01-31"}
               """;
    }
}
