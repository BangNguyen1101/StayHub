package com.quanlynhatro.maintenance.api;

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
import com.quanlynhatro.maintenance.service.MaintenanceService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

@WebMvcTest(MaintenanceController.class)
@Import({SecurityConfig.class, AuthExceptionHandler.class})
class MaintenanceControllerSecurityTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean MaintenanceService maintenance;
    @MockitoBean AppUserRepository users;
    @MockitoBean AppUserDetailsService details;
    @MockitoBean JwtService jwt;

    @Test
    void unauthenticatedCannotUseMaintenanceApis() throws Exception {
        mockMvc.perform(get("/api/maintenance-requests")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/maintenance-requests"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/maintenance-assignees"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tenantCannotUseAdminMaintenanceApis() throws Exception {
        mockMvc.perform(get("/api/admin/maintenance-requests").with(user("tenant").roles("TENANT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        patch("/api/admin/maintenance-requests/1")
                                .with(user("tenant").roles("TENANT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        get("/api/admin/maintenance-assignees")
                                .with(user("tenant").roles("TENANT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void tenantAndAdminRoutesUseAuthenticatedIdentity() throws Exception {
        AppUserEntity admin = mockUser(1L, "ADMIN"), tenant = mockUser(2L, "TENANT");
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("admin", "admin"))
                .thenReturn(Optional.of(admin));
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("tenant", "tenant"))
                .thenReturn(Optional.of(tenant));
        when(maintenance.searchForAdmin(any(), eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(maintenance.searchForTenant(any(), eq(2L), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(users.findAllByRole_CodeAndActiveTrue("ADMIN")).thenReturn(List.of(admin));

        mockMvc.perform(get("/api/admin/maintenance-requests").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/maintenance-assignees").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/maintenance-requests").with(user("tenant").roles("TENANT")))
                .andExpect(status().isOk());
        mockMvc.perform(
                        post("/api/maintenance-requests")
                                .with(user("tenant").roles("TENANT"))
                                .contentType("application/json")
                                .content("{}"))
                .andExpect(status().isBadRequest());
        verify(maintenance, never()).createForTenant(any(), anyLong());
    }

    private AppUserEntity mockUser(Long id, String code) {
        AppUserEntity user = mock(AppUserEntity.class);
        RoleEntity role = mock(RoleEntity.class);
        when(user.getId()).thenReturn(id);
        when(user.getRole()).thenReturn(role);
        when(role.getCode()).thenReturn(code);
        when(user.isActive()).thenReturn(true);
        return user;
    }
}
