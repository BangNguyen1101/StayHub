package com.quanlynhatro.auth.service;

import com.quanlynhatro.auth.api.AuthDtos;
import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.domain.TenantProfileEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.auth.repository.RoleRepository;
import com.quanlynhatro.auth.repository.TenantProfileRepository;
import com.quanlynhatro.auth.security.JwtService;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final AppUserRepository users;
    private final RoleRepository roles;
    private final TenantProfileRepository profiles;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final UserDetailsService userDetailsService;

    public AuthService(
            AuthenticationManager authenticationManager,
            AppUserRepository users,
            RoleRepository roles,
            TenantProfileRepository profiles,
            PasswordEncoder encoder,
            JwtService jwt,
            UserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.users = users;
        this.roles = roles;
        this.profiles = profiles;
        this.encoder = encoder;
        this.jwt = jwt;
        this.userDetailsService = userDetailsService;
    }

    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.identifier(), request.password()));
        AppUserEntity user = findUser(request.identifier());
        user.updateLastLogin(LocalDateTime.now());
        UserDetails details = userDetailsService.loadUserByUsername(user.getUsername());
        return new AuthDtos.AuthResponse(jwt.generateToken(details), "Bearer", toResponse(user));
    }

    @Transactional
    public AuthDtos.UserResponse register(AuthDtos.RegisterRequest request) {
        if (users.existsByUsernameIgnoreCase(request.username())
                || users.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("Username or email already exists");
        }
        var tenantRole =
                roles.findByCode("TENANT")
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "TENANT role is missing from database"));
        AppUserEntity user =
                users.save(
                        AppUserEntity.createTenant(
                                request.username(),
                                request.email(),
                                encoder.encode(request.password()),
                                tenantRole,
                                request.fullName(),
                                request.phoneNumber()));
        profiles.save(TenantProfileEntity.createFor(user));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthDtos.UserResponse me(String username) {
        return toResponse(findUser(username));
    }

    private AppUserEntity findUser(String identifier) {
        return users.findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, identifier)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private AuthDtos.UserResponse toResponse(AppUserEntity user) {
        return new AuthDtos.UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getRole().getCode(),
                user.isActive());
    }
}
