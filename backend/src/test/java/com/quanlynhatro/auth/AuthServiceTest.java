package com.quanlynhatro.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.quanlynhatro.auth.api.AuthDtos;
import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.domain.RoleEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.auth.repository.RoleRepository;
import com.quanlynhatro.auth.repository.TenantProfileRepository;
import com.quanlynhatro.auth.security.JwtService;
import com.quanlynhatro.auth.service.AuthService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock AuthenticationManager authenticationManager;
    @Mock AppUserRepository users;
    @Mock RoleRepository roles;
    @Mock TenantProfileRepository profiles;
    @Mock PasswordEncoder encoder;
    @Mock JwtService jwt;
    @Mock UserDetailsService detailsService;
    @Mock RoleEntity tenantRole;
    @Mock AppUserEntity existingUser;
    private AuthService service;

    @BeforeEach
    void setUp() {
        service =
                new AuthService(
                        authenticationManager,
                        users,
                        roles,
                        profiles,
                        encoder,
                        jwt,
                        detailsService);
    }

    @Test
    void loginSuccessReturnsJwtWithoutPasswordHash() {
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("tenant", "tenant"))
                .thenReturn(Optional.of(existingUser));
        when(existingUser.getUsername()).thenReturn("tenant");
        when(existingUser.getRole()).thenReturn(tenantRole);
        when(existingUser.getRole().getCode()).thenReturn("TENANT");
        User details = (User) User.withUsername("tenant").password("hash").roles("TENANT").build();
        when(detailsService.loadUserByUsername("tenant")).thenReturn(details);
        when(jwt.generateToken(details)).thenReturn("jwt-token");
        when(existingUser.isActive()).thenReturn(true);

        AuthDtos.AuthResponse response =
                service.login(new AuthDtos.LoginRequest("tenant", "password123"));

        assertEquals("jwt-token", response.token());
        assertNotNull(response.user());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(existingUser).updateLastLogin(any());
    }

    @Test
    void loginDisabledAccountFailsBeforeIssuingToken() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new DisabledException("disabled"));
        assertThrows(
                DisabledException.class,
                () -> service.login(new AuthDtos.LoginRequest("disabled", "password123")));
        verifyNoInteractions(jwt);
    }

    @Test
    void loginBadPasswordFailsBeforeIssuingToken() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad password"));
        assertThrows(
                BadCredentialsException.class,
                () -> service.login(new AuthDtos.LoginRequest("tenant", "wrong")));
        verifyNoInteractions(jwt, users);
    }

    @Test
    void registerCreatesTenantOnlyAndHashesPassword() {
        when(users.existsByUsernameIgnoreCase("newtenant")).thenReturn(false);
        when(users.existsByEmailIgnoreCase("newtenant@example.com")).thenReturn(false);
        when(roles.findByCode("TENANT")).thenReturn(Optional.of(tenantRole));
        when(tenantRole.getCode()).thenReturn("TENANT");
        when(encoder.encode("password123")).thenReturn("bcrypt-hash");
        AppUserEntity saved = mock(AppUserEntity.class);
        when(users.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AuthDtos.UserResponse response =
                service.register(
                        new AuthDtos.RegisterRequest(
                                "newtenant",
                                "newtenant@example.com",
                                "password123",
                                "New Tenant",
                                "0900000000"));

        ArgumentCaptor<AppUserEntity> captor = ArgumentCaptor.forClass(AppUserEntity.class);
        verify(users).save(captor.capture());
        assertEquals("newtenant", captor.getValue().getUsername());
        assertEquals("newtenant@example.com", captor.getValue().getEmail());
        assertEquals("bcrypt-hash", captor.getValue().getPasswordHash());
        verify(profiles).save(any());
        assertNotNull(response);
        assertEquals("TENANT", response.role());
    }

    @Test
    void duplicateRegistrationDoesNotWrite() {
        when(users.existsByUsernameIgnoreCase("tenant")).thenReturn(true);
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        service.register(
                                new AuthDtos.RegisterRequest(
                                        "tenant",
                                        "tenant@example.com",
                                        "password123",
                                        "Tenant",
                                        null)));
        verify(users, never()).save(any());
        verifyNoInteractions(encoder, profiles, roles);
    }

    @Test
    void registerRequestCannotSelectAdminRole() {
        assertFalse(
                java.util.Arrays.stream(AuthDtos.RegisterRequest.class.getRecordComponents())
                        .anyMatch(component -> component.getName().equalsIgnoreCase("role")));
    }
}
