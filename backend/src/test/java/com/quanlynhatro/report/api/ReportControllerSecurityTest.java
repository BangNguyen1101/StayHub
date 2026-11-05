package com.quanlynhatro.report.api;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quanlynhatro.auth.api.AuthExceptionHandler;
import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.domain.RoleEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.auth.security.*;
import com.quanlynhatro.report.service.ReportService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

@WebMvcTest(ReportController.class)
@Import({SecurityConfig.class, AuthExceptionHandler.class})
class ReportControllerSecurityTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean ReportService reports;
    @MockitoBean AppUserRepository users;
    @MockitoBean AppUserDetailsService details;
    @MockitoBean JwtService jwt;

    @Test
    void unauthenticatedCannotAccessReports() throws Exception {
        mockMvc.perform(get("/api/admin/reports/dashboard")).andExpect(status().isUnauthorized());
    }

    @Test
    void tenantCannotAccessReports() throws Exception {
        mockMvc.perform(get("/api/admin/reports/dashboard").with(user("tenant").roles("TENANT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessDashboard() throws Exception {
        AppUserEntity admin = mock(AppUserEntity.class);
        RoleEntity role = mock(RoleEntity.class);
        when(admin.getRole()).thenReturn(role);
        when(role.getCode()).thenReturn("ADMIN");
        when(admin.isActive()).thenReturn(true);
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("admin", "admin"))
                .thenReturn(Optional.of(admin));
        when(reports.dashboard(any()))
                .thenReturn(
                        new ReportDtos.Dashboard(
                                0,
                                0,
                                0,
                                java.math.BigDecimal.ZERO,
                                0,
                                0,
                                0,
                                0,
                                0,
                                java.util.List.of(),
                                0,
                                0,
                                java.math.BigDecimal.ZERO));
        mockMvc.perform(get("/api/admin/reports/dashboard").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void configuredFrontendOriginCanPreflightReports() throws Exception {
        mockMvc.perform(
                        options("/api/admin/reports/dashboard")
                                .header("Origin", "http://localhost:5173")
                                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                                .string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
