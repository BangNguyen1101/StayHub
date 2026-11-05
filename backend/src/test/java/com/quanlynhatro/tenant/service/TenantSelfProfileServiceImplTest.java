package com.quanlynhatro.tenant.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.domain.RoleEntity;
import com.quanlynhatro.auth.domain.TenantProfileEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.auth.repository.TenantProfileRepository;
import com.quanlynhatro.tenant.api.TenantSelfProfileUpdateRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class TenantSelfProfileServiceImplTest {
    @Mock private AppUserRepository users;
    @Mock private TenantProfileRepository profiles;

    @Test
    void tenantCanUpdateOnlySelfEditableFields() {
        AppUserEntity user = org.mockito.Mockito.mock(AppUserEntity.class);
        RoleEntity role = org.mockito.Mockito.mock(RoleEntity.class);
        TenantProfileEntity profile = org.mockito.Mockito.mock(TenantProfileEntity.class);
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("tenant01", "tenant01"))
                .thenReturn(Optional.of(user));
        when(user.getRole()).thenReturn(role);
        when(role.getCode()).thenReturn("TENANT");
        when(user.getId()).thenReturn(7L);
        when(profiles.findByUserIdAndUser_Role_Code(7L, "TENANT")).thenReturn(Optional.of(profile));
        when(user.getUsername()).thenReturn("tenant01");
        when(user.getEmail()).thenReturn("tenant@example.com");
        when(user.getFullName()).thenReturn("Tenant One");
        when(user.getPhoneNumber()).thenReturn("0900000000");
        when(user.isActive()).thenReturn(true);
        when(profile.getContactAddress()).thenReturn("Ha Noi");

        var request =
                new TenantSelfProfileUpdateRequest(" Tenant Updated ", " 0900000001 ", " Ha Noi ");
        new TenantSelfProfileServiceImpl(users, profiles).updateMyProfile("tenant01", request);

        verify(user).updateTenantProfile("Tenant Updated", "0900000001");
        verify(profile).updateContactAddress("Ha Noi");
    }

    @Test
    void nonTenantCannotUseSelfProfileService() {
        AppUserEntity user = org.mockito.Mockito.mock(AppUserEntity.class);
        RoleEntity role = org.mockito.Mockito.mock(RoleEntity.class);
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("admin", "admin"))
                .thenReturn(Optional.of(user));
        when(user.getRole()).thenReturn(role);
        when(role.getCode()).thenReturn("ADMIN");

        assertThrows(
                TenantNotFoundException.class,
                () -> new TenantSelfProfileServiceImpl(users, profiles).getMyProfile("admin"));
    }

    @Test
    void selfProfileFailsWhenTenantProfileIsMissing() {
        AppUserEntity user = org.mockito.Mockito.mock(AppUserEntity.class);
        RoleEntity role = org.mockito.Mockito.mock(RoleEntity.class);
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("tenant01", "tenant01"))
                .thenReturn(Optional.of(user));
        when(user.getRole()).thenReturn(role);
        when(role.getCode()).thenReturn("TENANT");
        when(user.getId()).thenReturn(7L);
        when(profiles.findByUserIdAndUser_Role_Code(7L, "TENANT")).thenReturn(Optional.empty());

        assertThrows(
                TenantProfileNotFoundException.class,
                () -> new TenantSelfProfileServiceImpl(users, profiles).getMyProfile("tenant01"));
    }
}
