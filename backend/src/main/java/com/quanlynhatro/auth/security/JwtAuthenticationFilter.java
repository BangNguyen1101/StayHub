package com.quanlynhatro.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtService jwt;
    private final AppUserDetailsService users;

    public JwtAuthenticationFilter(JwtService jwt, AppUserDetailsService users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null
                && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String token = header.substring(7);
                UserDetails details = users.loadUserByUsername(jwt.extractUsername(token));
                if (jwt.isValid(token, details) && details.isEnabled()) {
                    var authentication =
                            new UsernamePasswordAuthenticationToken(
                                    details, null, details.getAuthorities());
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    log.warn(
                            "JWT rejected for protected request {}: token is invalid, expired, or"
                                + " belongs to a disabled user",
                            request.getRequestURI());
                }
            } catch (RuntimeException ex) {
                log.warn(
                        "JWT validation failed for protected request {}: {}",
                        request.getRequestURI(),
                        ex.getClass().getSimpleName());
                // Leave the request unauthenticated; the entry point returns 401 for protected
                // resources.
            }
        }
        chain.doFilter(request, response);
    }
}
