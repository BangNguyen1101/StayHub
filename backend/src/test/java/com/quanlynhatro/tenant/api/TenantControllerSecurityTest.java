package com.quanlynhatro.tenant.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quanlynhatro.auth.api.AuthExceptionHandler;
import com.quanlynhatro.auth.security.AppUserDetailsService;
import com.quanlynhatro.auth.security.JwtService;
import com.quanlynhatro.auth.security.SecurityConfig;
import com.quanlynhatro.tenant.service.DuplicateTenantEmailException;
import com.quanlynhatro.tenant.service.TenantManagementService;
import com.quanlynhatro.tenant.service.TenantSelfProfileService;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@WebMvcTest(TenantController.class)
@Import({SecurityConfig.class, AuthExceptionHandler.class})
class TenantControllerSecurityTest {
    @Autowired private MockMvc mockMvc;

    @MockitoBean private TenantManagementService tenantManagementService;
    @MockitoBean private TenantSelfProfileService tenantSelfProfileService;
    @MockitoBean private AppUserDetailsService userDetailsService;
    @MockitoBean private JwtService jwtService;

    @Test
    void unauthenticatedCannotAccessTenantManagementApis() throws Exception {
        mockMvc.perform(get("/api/admin/tenants")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/tenant/profile")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanDeactivateTenant() throws Exception {
        mockMvc.perform(
                        delete("/api/admin/tenants/7")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.user("admin")
                                                .roles("ADMIN")))
                .andExpect(status().isNoContent());
        verify(tenantManagementService).deactivate(7L);
    }

    @Test
    void tenantCannotDeactivateAnotherTenant() throws Exception {
        mockMvc.perform(
                        delete("/api/admin/tenants/7")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.user("tenant01")
                                                .roles("TENANT")))
                .andExpect(status().isForbidden());
        verify(tenantManagementService, never()).deactivate(7L);
    }

    @Test
    void tenantCannotAccessAdminApis() throws Exception {
        mockMvc.perform(
                        get("/api/admin/tenants")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.user("tenant01")
                                                .roles("TENANT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(
                        patch("/api/admin/tenants/7/profile")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.user("tenant01")
                                                .roles("TENANT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotAccessTenantSelfProfileApi() throws Exception {
        mockMvc.perform(
                        get("/api/tenant/profile")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.user("admin")
                                                .roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanSearchTenantsWithPageResponse() throws Exception {
        when(tenantManagementService.search(eq("tenant"), eq(true), any()))
                .thenReturn(new PageImpl<>(List.of(summary())));

        mockMvc.perform(
                        get("/api/admin/tenants")
                                .param("keyword", "tenant")
                                .param("active", "true")
                                .param("page", "0")
                                .param("size", "10")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.user("admin")
                                                .roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("tenant01"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
    }

    @Test
    void adminCanReadAndUpdateTenantProfile() throws Exception {
        when(tenantManagementService.getById(7L)).thenReturn(detail());
        when(tenantManagementService.update(eq(7L), any(AdminTenantProfileUpdateRequest.class)))
                .thenReturn(detail());

        mockMvc.perform(
                        get("/api/admin/tenants/7")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.user("admin")
                                                .roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("tenant@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mockMvc.perform(
                        patch("/api/admin/tenants/7/profile")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.user("admin")
                                                .roles("ADMIN"))
                                .contentType("application/json")
                                .content(adminUpdateJson()))
                .andExpect(status().isOk());
    }

    @Test
    void adminDetailNotFoundUsesUnifiedNotFoundResponse() throws Exception {
        when(tenantManagementService.getById(99L))
                .thenThrow(new com.quanlynhatro.tenant.service.TenantNotFoundException(99L));

        mockMvc.perform(
                        get("/api/admin/tenants/99")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.user("admin")
                                                .roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TENANT_NOT_FOUND"));
    }

    @Test
    void nonPositiveTenantIdIsRejectedBeforeCallingService() throws Exception {
        mockMvc.perform(
                        get("/api/admin/tenants/0")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.user("admin")
                                                .roles("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verify(tenantManagementService, never()).getById(0L);
    }

    @Test
    void invalidAdminProfileReturnsValidationError() throws Exception {
        mockMvc.perform(
                        patch("/api/admin/tenants/7/profile")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.user("admin")
                                                .roles("ADMIN"))
                                .contentType("application/json")
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verify(tenantManagementService, never()).update(any(), any());
    }

    @Test
    void duplicateAdminEmailReturnsConflict() throws Exception {
        when(tenantManagementService.update(eq(7L), any(AdminTenantProfileUpdateRequest.class)))
                .thenThrow(new DuplicateTenantEmailException("duplicate@example.com"));

        mockMvc.perform(
                        patch("/api/admin/tenants/7/profile")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.user("admin")
                                                .roles("ADMIN"))
                                .contentType("application/json")
                                .content(adminUpdateJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TENANT_EMAIL_EXISTS"));
    }

    @Test
    void tenantProfileUsesAuthenticatedNameAndDoesNotAcceptUserId() throws Exception {
        when(tenantSelfProfileService.getMyProfile("tenant01")).thenReturn(profile());
        when(tenantSelfProfileService.updateMyProfile(
                        eq("tenant01"), any(TenantSelfProfileUpdateRequest.class)))
                .thenReturn(profile());

        mockMvc.perform(
                        get("/api/tenant/profile")
                                .param("userId", "999")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.user("tenant01")
                                                .roles("TENANT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("tenant01"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mockMvc.perform(
                        patch("/api/tenant/profile")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.user("tenant01")
                                                .roles("TENANT"))
                                .contentType("application/json")
                                .content(selfUpdateJson()))
                .andExpect(status().isOk());
        verify(tenantSelfProfileService).getMyProfile("tenant01");
        verify(tenantSelfProfileService)
                .updateMyProfile(eq("tenant01"), any(TenantSelfProfileUpdateRequest.class));
    }

    @Test
    void tenantCannotSubmitAdminFieldsThroughSelfProfileRequest() throws Exception {
        mockMvc.perform(
                        patch("/api/tenant/profile")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.user("tenant01")
                                                .roles("TENANT"))
                                .contentType("application/json")
                                .content(
                                        "{\"fullName\":\"Tenant\",\"email\":\"changed@example.com\",\"identityDocumentNumber\":\"123\"}"))
                .andExpect(status().isOk());
        ArgumentCaptor<TenantSelfProfileUpdateRequest> requestCaptor =
                ArgumentCaptor.forClass(TenantSelfProfileUpdateRequest.class);
        verify(tenantSelfProfileService).updateMyProfile(eq("tenant01"), requestCaptor.capture());
        assertEquals("Tenant", requestCaptor.getValue().fullName());
    }

    @Test
    void adminSystemFieldsAreNotBoundToAdminProfileDto() throws Exception {
        when(tenantManagementService.update(eq(7L), any(AdminTenantProfileUpdateRequest.class)))
                .thenReturn(detail());

        mockMvc.perform(
                        patch("/api/admin/tenants/7/profile")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.user("admin")
                                                .roles("ADMIN"))
                                .contentType("application/json")
                                .content(adminUpdateJsonWithSystemFields()))
                .andExpect(status().isOk());

        ArgumentCaptor<AdminTenantProfileUpdateRequest> requestCaptor =
                ArgumentCaptor.forClass(AdminTenantProfileUpdateRequest.class);
        verify(tenantManagementService).update(eq(7L), requestCaptor.capture());
        AdminTenantProfileUpdateRequest request = requestCaptor.getValue();
        assertEquals("tenant@example.com", request.email());
        assertNull(request.identityDocumentType());
    }

    private TenantSummaryResponse summary() {
        return new TenantSummaryResponse(
                7L, "tenant01", "tenant@example.com", "Tenant One", "0900000000", "Ha Noi", true);
    }

    private TenantDetailResponse detail() {
        return new TenantDetailResponse(
                7L,
                "tenant01",
                "tenant@example.com",
                "Tenant One",
                "0900000000",
                true,
                "Ha Noi",
                "CCCD",
                "123",
                LocalDate.of(2020, 1, 1),
                "Ha Noi",
                "note",
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    private TenantProfileResponse profile() {
        return new TenantProfileResponse(
                7L, "tenant01", "tenant@example.com", "Tenant One", "0900000000", "Ha Noi", true);
    }

    private String adminUpdateJson() {
        return """
               {
                 "email":"tenant@example.com",
                 "fullName":"Tenant One",
                 "phoneNumber":"0900000000",
                 "contactAddress":"Ha Noi",
                 "identityDocumentType":"CCCD",
                 "identityDocumentNumber":"123",
                 "identityIssuedPlace":"Ha Noi",
                 "notes":"note"
               }
               """;
    }

    private String adminUpdateJsonWithSystemFields() {
        return """
               {
                 "email":"tenant@example.com",
                 "fullName":"Tenant One",
                 "roleId":1,
                 "isActive":false,
                 "passwordHash":"should-not-bind"
               }
               """;
    }

    private String selfUpdateJson() {
        return """
               {"fullName":"Tenant Updated","phoneNumber":"0900000001","contactAddress":"Ha Noi"}
               """;
    }
}
