package com.quanlynhatro.auth.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class SecurityConfig {
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(
                Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(origin -> !origin.isBlank())
                        .toList());
        configuration.setAllowedMethods(
                Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationProvider authenticationProvider(
            AppUserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(users);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwt,
            AppUserDetailsService users,
            AuthenticationProvider provider)
            throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwt, users);
        return http.csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(provider)
                .authorizeHttpRequests(
                        auth ->
                                auth.dispatcherTypeMatchers(DispatcherType.ERROR)
                                        .permitAll()
                                        .requestMatchers("/error", "/api/auth/login", "/api/health")
                                        .permitAll()
                                        .requestMatchers("/api/auth/register")
                                        .hasRole("ADMIN")
                                        .requestMatchers("/api/assistant/**")
                                        .hasAnyRole("ADMIN", "TENANT")
                                        .requestMatchers("/api/admin/rooms/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers("/api/admin/tenants/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers("/api/admin/rental-requests/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers("/api/admin/contracts/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers("/api/admin/fee-configs/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers("/api/admin/services/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers("/api/admin/utility-readings/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers(
                                                "/api/admin/invoices/**", "/api/admin/debts/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers("/api/admin/payments/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers("/api/admin/notifications/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers("/api/admin/maintenance-assignees")
                                        .hasRole("ADMIN")
                                        .requestMatchers("/api/admin/maintenance-requests/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers("/api/admin/reports/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers("/api/rental-requests/**")
                                        .hasRole("TENANT")
                                        .requestMatchers("/api/tenant/contracts/**")
                                        .hasRole("TENANT")
                                        .requestMatchers(
                                                "/api/tenant/invoices/**", "/api/tenant/debts/**")
                                        .hasRole("TENANT")
                                        .requestMatchers("/api/tenant/payments/**")
                                        .hasRole("TENANT")
                                        .requestMatchers("/api/tenant/notifications/**")
                                        .hasRole("TENANT")
                                        .requestMatchers("/api/maintenance-requests/**")
                                        .hasRole("TENANT")
                                        .requestMatchers("/api/tenant/profile")
                                        .hasRole("TENANT")
                                        .requestMatchers(HttpMethod.GET, "/api/rooms/available")
                                        .hasAnyRole("ADMIN", "TENANT")
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        e ->
                                e.authenticationEntryPoint(
                                                (req, res, ex) -> {
                                                    res.setStatus(
                                                            HttpServletResponse.SC_UNAUTHORIZED);
                                                    res.setContentType("application/json");
                                                    res.getWriter()
                                                            .write(
                                                                    "{\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication"
                                                                        + " required\"}");
                                                })
                                        .accessDeniedHandler(
                                                (req, res, ex) -> {
                                                    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                                    res.setContentType("application/json");
                                                    res.getWriter()
                                                            .write(
                                                                    "{\"code\":\"FORBIDDEN\",\"message\":\"Access"
                                                                        + " denied\"}");
                                                }))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
