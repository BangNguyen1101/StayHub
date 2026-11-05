package com.quanlynhatro.auth;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.quanlynhatro.auth.api.AuthController;
import com.quanlynhatro.auth.api.AuthDtos;
import com.quanlynhatro.auth.security.AppUserDetailsService;
import com.quanlynhatro.auth.security.JwtService;
import com.quanlynhatro.auth.security.SecurityConfig;
import com.quanlynhatro.auth.service.AuthService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(AuthController.class)
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
@Import({
    SecurityConfig.class,
    SecurityMockMvcTest.AdminOnlyTestController.class,
    com.quanlynhatro.auth.api.AuthExceptionHandler.class
})
class SecurityMockMvcTest {
    @Autowired org.springframework.test.web.servlet.MockMvc mockMvc;
    @MockitoBean AuthService authService;
    @MockitoBean AppUserDetailsService userDetailsService;
    @MockitoBean JwtService jwtService;

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void tenantCanReadOwnProfileWithoutPasswordHash() throws Exception {
        when(authService.me("tenant"))
                .thenReturn(
                        new AuthDtos.UserResponse(
                                1L,
                                "tenant",
                                "tenant@example.com",
                                "Tenant",
                                null,
                                "TENANT",
                                true));
        mockMvc.perform(get("/api/auth/me").with(user("tenant").roles("TENANT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("tenant"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void tenantCannotAccessAdminOnlyTestController() throws Exception {
        mockMvc.perform(get("/test-only/admin").with(user("tenant").roles("TENANT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAdminOnlyTestController() throws Exception {
        mockMvc.perform(get("/test-only/admin").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @RestController
    static class AdminOnlyTestController {
        @GetMapping("/test-only/admin")
        @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
        String admin() {
            return "ok";
        }
    }
}
