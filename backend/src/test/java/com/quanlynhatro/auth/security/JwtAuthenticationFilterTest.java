package com.quanlynhatro.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

class JwtAuthenticationFilterTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenForDisabledUserRemainsUnauthenticated() throws Exception {
        JwtService jwt = mock(JwtService.class);
        AppUserDetailsService users = mock(AppUserDetailsService.class);
        var details =
                User.withUsername("tenant01")
                        .password("hash")
                        .roles("TENANT")
                        .disabled(true)
                        .build();
        when(jwt.extractUsername("token")).thenReturn("tenant01");
        when(users.loadUserByUsername("tenant01")).thenReturn(details);
        when(jwt.isValid("token", details)).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");

        new JwtAuthenticationFilter(jwt, users)
                .doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
