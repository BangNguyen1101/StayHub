package com.quanlynhatro.contract.service;

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
import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.domain.RoleEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.auth.security.AppUserDetailsService;
import com.quanlynhatro.auth.security.JwtService;
import com.quanlynhatro.auth.security.SecurityConfig;
import com.quanlynhatro.contract.api.ContractController;
import com.quanlynhatro.contract.api.ContractCreateRequest;
import com.quanlynhatro.contract.api.ContractResponse;
import com.quanlynhatro.contract.api.ContractStatus;
import com.quanlynhatro.contract.api.ContractSummaryResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@WebMvcTest(ContractController.class)
@Import({SecurityConfig.class, AuthExceptionHandler.class})
class ContractControllerSecurityTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private ContractManagementService contractService;
    @MockitoBean private ContractOccupantService occupantService;
    @MockitoBean private AppUserRepository users;
    @MockitoBean private AppUserDetailsService userDetailsService;
    @MockitoBean private JwtService jwtService;

    @Test
    void protectedEndpointsReturn401WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/contracts")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/tenant/contracts")).andExpect(status().isUnauthorized());
        mockMvc.perform(
                        post("/api/admin/contracts")
                                .contentType("application/json")
                                .content(validCreateJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rolesAreSeparatedBySpringSecurity() throws Exception {
        mockMvc.perform(get("/api/admin/contracts").with(user("tenant").roles("TENANT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/tenant/contracts").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminSearchAndDetailUseUnifiedResponses() throws Exception {
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("admin", "admin"))
                .thenAnswer(invocation -> Optional.of(admin(1L)));
        when(contractService.search(
                        eq("P01"),
                        eq(ContractStatus.ACTIVE),
                        eq(7L),
                        eq(5L),
                        any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(summary())));
        when(contractService.getById(1L)).thenReturn(response());

        mockMvc.perform(
                        get("/api/admin/contracts")
                                .param("keyword", "P01")
                                .param("status", "ACTIVE")
                                .param("tenantUserId", "7")
                                .param("roomId", "5")
                                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].contractCode").value("HD-001"));
        mockMvc.perform(get("/api/admin/contracts/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contractCode").value("HD-001"));
    }

    @Test
    void tenantListIsScopedToAuthenticatedUser() throws Exception {
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("tenant", "tenant"))
                .thenAnswer(invocation -> Optional.of(tenant(7L)));
        when(contractService.search(null, null, 7L, null, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(summary())));
        when(contractService.getForTenant("tenant", 1L)).thenReturn(response());

        mockMvc.perform(
                        get("/api/tenant/contracts")
                                .param("page", "0")
                                .param("size", "20")
                                .with(user("tenant").roles("TENANT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tenantUserId").value(7));
        mockMvc.perform(get("/api/tenant/contracts/1").with(user("tenant").roles("TENANT")))
                .andExpect(status().isOk());
        verify(contractService).getForTenant("tenant", 1L);
    }

    @Test
    void adminCanCreateAndSubmitContract() throws Exception {
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("admin", "admin"))
                .thenAnswer(invocation -> Optional.of(admin(1L)));
        when(contractService.create(any(ContractCreateRequest.class), eq(1L)))
                .thenReturn(response());
        when(contractService.submitPending(1L, 1L)).thenReturn(response());

        mockMvc.perform(
                        post("/api/admin/contracts")
                                .with(user("admin").roles("ADMIN"))
                                .contentType("application/json")
                                .content(validCreateJson()))
                .andExpect(status().isCreated());
        mockMvc.perform(patch("/api/admin/contracts/1/submit").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void invalidCreateIsRejectedBeforeServiceCall() throws Exception {
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("admin", "admin"))
                .thenAnswer(invocation -> Optional.of(admin(1L)));
        mockMvc.perform(
                        post("/api/admin/contracts")
                                .with(user("admin").roles("ADMIN"))
                                .contentType("application/json")
                                .content("{\"contractCode\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verify(contractService, never()).create(any(), any());
    }

    @Test
    void occupantAndHistoryEndpointsAreAdminOnly() throws Exception {
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("admin", "admin"))
                .thenAnswer(invocation -> Optional.of(admin(1L)));
        when(occupantService.list(1L)).thenReturn(List.of());
        when(contractService.history(1L)).thenReturn(List.of());
        mockMvc.perform(get("/api/admin/contracts/1/occupants").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/contracts/1/history").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void notFoundAndOwnershipErrorsUseUnifiedCodes() throws Exception {
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("admin", "admin"))
                .thenAnswer(invocation -> Optional.of(admin(1L)));
        when(contractService.getById(99L)).thenThrow(new ContractNotFoundException(99L));
        mockMvc.perform(get("/api/admin/contracts/99").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONTRACT_NOT_FOUND"));

        when(contractService.getForTenant("tenant", 99L))
                .thenThrow(new ContractAccessDeniedException());
        mockMvc.perform(get("/api/tenant/contracts/99").with(user("tenant").roles("TENANT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CONTRACT_ACCESS_DENIED"));
    }

    private AppUserEntity admin(Long id) {
        AppUserEntity user = org.mockito.Mockito.mock(AppUserEntity.class);
        RoleEntity role = org.mockito.Mockito.mock(RoleEntity.class);
        when(role.getCode()).thenReturn("ADMIN");
        when(user.getId()).thenReturn(id);
        when(user.getRole()).thenReturn(role);
        return user;
    }

    private AppUserEntity tenant(Long id) {
        AppUserEntity user = org.mockito.Mockito.mock(AppUserEntity.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    private ContractSummaryResponse summary() {
        return new ContractSummaryResponse(
                1L,
                "HD-001",
                7L,
                "Tenant",
                5L,
                "P01",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                new BigDecimal("3000000"),
                ContractStatus.ACTIVE);
    }

    private ContractResponse response() {
        return new ContractResponse(
                1L,
                "HD-001",
                7L,
                5L,
                "A102",
                null,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                null,
                new BigDecimal("3000000"),
                new BigDecimal("3000000"),
                1,
                (short) 5,
                ContractStatus.ACTIVE,
                null,
                null,
                LocalDateTime.now(),
                null,
                null,
                null);
    }

    private String validCreateJson() {
        return """
               {"contractCode":"HD-001","tenantUserId":7,"roomId":5,
                "startDate":"2026-01-01","endDate":"2026-12-31",
                "monthlyRent":3000000,"depositAmount":3000000,
                "billingCycleMonths":1,"dueDay":5}
               """;
    }
}
