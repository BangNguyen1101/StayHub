package com.quanlynhatro.notification.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quanlynhatro.auth.api.AuthExceptionHandler;
import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.domain.RoleEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.auth.security.AppUserDetailsService;
import com.quanlynhatro.auth.security.JwtService;
import com.quanlynhatro.auth.security.SecurityConfig;
import com.quanlynhatro.notification.service.NotificationService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, AuthExceptionHandler.class})
class NotificationControllerSecurityTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean NotificationService notifications;
    @MockitoBean AppUserRepository users;
    @MockitoBean AppUserDetailsService details;
    @MockitoBean JwtService jwt;

    @Test
    void unauthenticatedCannotUseNotificationApis() throws Exception {
        mockMvc.perform(get("/api/tenant/notifications")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/admin/notifications")).andExpect(status().isUnauthorized());
    }

    @Test
    void tenantCannotCreateOrListAdminNotifications() throws Exception {
        mockMvc.perform(post("/api/admin/notifications").with(user("tenant").roles("TENANT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/notifications").with(user("tenant").roles("TENANT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void tenantAndAdminRoutesUseAuthenticatedIdentity() throws Exception {
        AppUserEntity admin = mockUser(1L, "ADMIN");
        AppUserEntity tenant = mockUser(2L, "TENANT");
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("admin", "admin"))
                .thenReturn(Optional.of(admin));
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("tenant", "tenant"))
                .thenReturn(Optional.of(tenant));
        when(notifications.searchForAdmin(any(), eq(1L))).thenReturn(new PageImpl<>(List.of()));
        when(notifications.searchForTenant(any(), eq(2L), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(notifications.unreadCount(2L)).thenReturn(0L);

        mockMvc.perform(get("/api/admin/notifications").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tenant/notifications").with(user("tenant").roles("TENANT")))
                .andExpect(status().isOk());
        mockMvc.perform(
                        get("/api/tenant/notifications/unread-count")
                                .with(user("tenant").roles("TENANT")))
                .andExpect(status().isOk());
        mockMvc.perform(
                        post("/api/admin/notifications")
                                .with(user("admin").roles("ADMIN"))
                                .contentType("application/json")
                                .content("{}"))
                .andExpect(status().isBadRequest());
        verify(notifications, never()).create(any(), anyLong());
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
