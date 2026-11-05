package com.quanlynhatro.billing.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quanlynhatro.auth.api.AuthExceptionHandler;
import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.domain.RoleEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.auth.security.*;
import com.quanlynhatro.billing.service.PaymentService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

@WebMvcTest(PaymentController.class)
@Import({SecurityConfig.class, AuthExceptionHandler.class})
class PaymentControllerSecurityTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean PaymentService payments;
    @MockitoBean AppUserRepository users;
    @MockitoBean AppUserDetailsService details;
    @MockitoBean JwtService jwt;

    @Test
    void unauthenticatedCannotUsePaymentApis() throws Exception {
        mockMvc.perform(get("/api/admin/payments")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/tenant/payments")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/tenant/invoices/1/payment-qr"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tenantCannotUseAdminPaymentApis() throws Exception {
        mockMvc.perform(get("/api/admin/payments").with(user("tenant").roles("TENANT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        post("/api/admin/invoices/1/payments/manual")
                                .with(user("tenant").roles("TENANT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminAndTenantRoutesUseExpectedServices() throws Exception {
        AppUserEntity admin = mockUser(9L, "ADMIN"), tenant = mockUser(10L, "TENANT");
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("admin", "admin"))
                .thenReturn(Optional.of(admin));
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("tenant", "tenant"))
                .thenReturn(Optional.of(tenant));
        when(payments.searchAdmin(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(payments.searchTenant(eq(10L), any())).thenReturn(new PageImpl<>(List.of()));
        when(payments.invoiceHistory(anyLong(), any(), anyBoolean(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/api/admin/payments").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/invoices/1/payments").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tenant/payments").with(user("tenant").roles("TENANT")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tenant/invoices/1/payments").with(user("tenant").roles("TENANT")))
                .andExpect(status().isOk());
        mockMvc.perform(
                        post("/api/admin/invoices/1/payments/manual")
                                .with(user("admin").roles("ADMIN"))
                                .contentType("application/json")
                                .content("{}"))
                .andExpect(status().isBadRequest());
        verify(payments, never()).recordManual(anyLong(), any(), anyLong());
    }

    private AppUserEntity mockUser(Long id, String code) {
        AppUserEntity user = mock(AppUserEntity.class);
        RoleEntity role = mock(RoleEntity.class);
        when(user.getId()).thenReturn(id);
        when(user.getRole()).thenReturn(role);
        when(role.getCode()).thenReturn(code);
        return user;
    }
}
