package com.quanlynhatro.billing.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quanlynhatro.auth.api.AuthExceptionHandler;
import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.domain.RoleEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.auth.security.AppUserDetailsService;
import com.quanlynhatro.auth.security.JwtService;
import com.quanlynhatro.auth.security.SecurityConfig;
import com.quanlynhatro.billing.service.BillingService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

@WebMvcTest(BillingController.class)
@Import({SecurityConfig.class, AuthExceptionHandler.class})
class BillingControllerSecurityTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean BillingService billing;
    @MockitoBean AppUserRepository users;
    @MockitoBean AppUserDetailsService userDetailsService;
    @MockitoBean JwtService jwtService;

    @Test
    void unauthenticatedCannotAccessBillingApis() throws Exception {
        mockMvc.perform(get("/api/admin/invoices")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/debts")).andExpect(status().isUnauthorized());
    }

    @Test
    void tenantCannotAccessAdminBillingApis() throws Exception {
        mockMvc.perform(get("/api/admin/invoices").with(user("tenant").roles("TENANT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/debts").with(user("tenant").roles("TENANT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanSearchInvoicesAndInvalidCreateIsRejected() throws Exception {
        AppUserEntity admin = mockUser(9L, "ADMIN");
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("admin", "admin"))
                .thenReturn(Optional.of(admin));
        when(billing.search(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/invoices").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(
                        post("/api/admin/invoices")
                                .with(user("admin").roles("ADMIN"))
                                .contentType(APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
        verify(billing, never()).createDraft(any(), anyLong());
    }

    @Test
    void tenantCanReadOwnInvoiceButCannotReadAnotherTenantInvoice() throws Exception {
        AppUserEntity tenant = mockUser(10L, "TENANT");
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("tenant", "tenant"))
                .thenReturn(Optional.of(tenant));
        when(billing.getTenant(eq(1L), eq(10L)))
                .thenReturn(
                        new InvoiceResponse(
                                null, null, null, null, "A102", 10L, null, null, null, null, null,
                                null, null, null, null, null, null, null, null, List.of()));
        mockMvc.perform(get("/api/tenant/invoices/1").with(user("tenant").roles("TENANT")))
                .andExpect(status().isOk());
        doThrow(new com.quanlynhatro.billing.service.InvoiceAccessDeniedException())
                .when(billing)
                .getTenant(2L, 10L);
        mockMvc.perform(get("/api/tenant/invoices/2").with(user("tenant").roles("TENANT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void remainingBillingReadAndIssueRoutesAreCovered() throws Exception {
        AppUserEntity admin = mockUser(9L, "ADMIN");
        AppUserEntity tenant = mockUser(10L, "TENANT");
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("admin", "admin"))
                .thenReturn(Optional.of(admin));
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("tenant", "tenant"))
                .thenReturn(Optional.of(tenant));
        when(billing.search(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(billing.debts(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(billing.searchForTenant(eq(10L), any())).thenReturn(new PageImpl<>(List.of()));
        when(billing.issueDrafts("2026-11", 9L)).thenReturn(2);
        when(billing.getAdmin(1L)).thenReturn(null);
        when(billing.issue(1L, 9L)).thenReturn(null);

        mockMvc.perform(get("/api/admin/invoices/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/admin/invoices/1/issue").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/debts").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tenant/invoices").with(user("tenant").roles("TENANT")))
                .andExpect(status().isOk());
        // Tenants only get issued invoices, never the admin's full search with drafts.
        verify(billing).searchForTenant(eq(10L), any());
        mockMvc.perform(get("/api/tenant/debts").with(user("tenant").roles("TENANT")))
                .andExpect(status().isOk());
        mockMvc.perform(
                        patch("/api/admin/invoices/issue-drafts")
                                .param("period", "2026-11")
                                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issued").value(2));
        mockMvc.perform(
                        patch("/api/admin/invoices/issue-drafts")
                                .with(user("tenant").roles("TENANT")))
                .andExpect(status().isForbidden());
    }

    private AppUserEntity mockUser(Long id, String roleCode) {
        AppUserEntity user = mock(AppUserEntity.class);
        RoleEntity role = mock(RoleEntity.class);
        when(user.getId()).thenReturn(id);
        when(user.getRole()).thenReturn(role);
        when(role.getCode()).thenReturn(roleCode);
        return user;
    }
}
