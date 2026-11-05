package com.quanlynhatro.tenant.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.domain.TenantProfileEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.auth.repository.RoleRepository;
import com.quanlynhatro.auth.repository.TenantProfileRepository;
import com.quanlynhatro.contract.api.ContractStatus;
import com.quanlynhatro.contract.repository.ContractRepository;
import com.quanlynhatro.tenant.api.AdminTenantProfileUpdateRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class TenantManagementServiceImplTest {
    @Mock private AppUserRepository users;
    @Mock private TenantProfileRepository profiles;
    @Mock private RoleRepository roles;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ContractRepository contracts;

    private TenantManagementServiceImpl service() {
        return new TenantManagementServiceImpl(users, profiles, roles, passwordEncoder, contracts);
    }

    @Test
    void deactivateDisablesTenantWithoutEffectiveContract() {
        AppUserEntity user = org.mockito.Mockito.mock(AppUserEntity.class);
        when(users.findByIdAndRole_Code(7L, "TENANT")).thenReturn(Optional.of(user));

        service().deactivate(7L);

        verify(user).deactivate();
    }

    @Test
    void deactivateRejectsTenantWithEffectiveContract() {
        AppUserEntity user = org.mockito.Mockito.mock(AppUserEntity.class);
        when(users.findByIdAndRole_Code(7L, "TENANT")).thenReturn(Optional.of(user));
        when(contracts.existsByTenantUser_IdAndStatusIn(
                        7L, Set.of(ContractStatus.ACTIVE, ContractStatus.EXPIRING)))
                .thenReturn(true);

        assertThrows(TenantDeletionConflictException.class, () -> service().deactivate(7L));
        verify(user, never()).deactivate();
    }

    @Test
    void searchReturnsTenantPageWithoutSensitiveFields() {
        AppUserEntity user = org.mockito.Mockito.mock(AppUserEntity.class);
        TenantProfileEntity profile = org.mockito.Mockito.mock(TenantProfileEntity.class);
        when(user.getId()).thenReturn(7L);
        when(user.getUsername()).thenReturn("tenant01");
        when(user.getEmail()).thenReturn("tenant@example.com");
        when(user.getFullName()).thenReturn("Tenant One");
        when(user.getPhoneNumber()).thenReturn("0900000000");
        when(user.isActive()).thenReturn(true);
        when(profile.getContactAddress()).thenReturn("Ha Noi");
        when(users.searchTenants("tenant", true, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(profiles.findByUserIdAndUser_Role_Code(7L, "TENANT")).thenReturn(Optional.of(profile));

        var result = service().search(" tenant ", true, Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        assertEquals("tenant01", result.getContent().getFirst().username());
        assertEquals("Ha Noi", result.getContent().getFirst().contactAddress());
    }

    @Test
    void updateRejectsDuplicateEmail() {
        AppUserEntity user = org.mockito.Mockito.mock(AppUserEntity.class);
        when(users.findByIdAndRole_Code(7L, "TENANT")).thenReturn(Optional.of(user));
        when(user.getEmail()).thenReturn("old@example.com");
        when(users.existsByEmailIgnoreCaseAndIdNot("new@example.com", 7L)).thenReturn(true);
        var request =
                new AdminTenantProfileUpdateRequest(
                        "new@example.com", "Tenant One", null, null, null, null, null, null, null);

        assertThrows(DuplicateTenantEmailException.class, () -> service().update(7L, request));
    }

    @Test
    void updateChangesAdminEditableFields() {
        AppUserEntity user = org.mockito.Mockito.mock(AppUserEntity.class);
        TenantProfileEntity profile = org.mockito.Mockito.mock(TenantProfileEntity.class);
        when(users.findByIdAndRole_Code(7L, "TENANT")).thenReturn(Optional.of(user));
        when(user.getEmail()).thenReturn("old@example.com");
        when(users.existsByEmailIgnoreCaseAndIdNot("new@example.com", 7L)).thenReturn(false);
        when(profiles.findByUserIdAndUser_Role_Code(7L, "TENANT")).thenReturn(Optional.of(profile));
        when(user.getId()).thenReturn(7L);
        when(user.getUsername()).thenReturn("tenant01");
        when(user.getFullName()).thenReturn("Tenant One");
        when(user.getPhoneNumber()).thenReturn("0900000000");
        when(user.isActive()).thenReturn(true);
        var request =
                new AdminTenantProfileUpdateRequest(
                        "new@example.com",
                        "Tenant One",
                        "0900000000",
                        "Ha Noi",
                        "CCCD",
                        "123",
                        null,
                        "Ha Noi",
                        "note");

        service().update(7L, request);

        verify(user).updateAdminProfile("new@example.com", "Tenant One", "0900000000");
        verify(profile).updateAdminFields("Ha Noi", "CCCD", "123", null, "Ha Noi", "note");
    }

    @Test
    void detailFailsWhenProfileIsMissing() {
        AppUserEntity user = org.mockito.Mockito.mock(AppUserEntity.class);
        when(users.findByIdAndRole_Code(7L, "TENANT")).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(7L);
        when(profiles.findByUserIdAndUser_Role_Code(7L, "TENANT")).thenReturn(Optional.empty());

        assertThrows(TenantProfileNotFoundException.class, () -> service().getById(7L));
    }

    @Test
    void adminUpdateFailsWhenTenantDoesNotExist() {
        when(users.findByIdAndRole_Code(99L, "TENANT")).thenReturn(Optional.empty());
        var request =
                new AdminTenantProfileUpdateRequest(
                        "tenant@example.com", "Tenant", null, null, null, null, null, null, null);

        assertThrows(TenantNotFoundException.class, () -> service().update(99L, request));
    }

    @Test
    void adminUpdateFailsWhenTenantProfileIsMissing() {
        AppUserEntity user = org.mockito.Mockito.mock(AppUserEntity.class);
        when(users.findByIdAndRole_Code(7L, "TENANT")).thenReturn(Optional.of(user));
        when(user.getEmail()).thenReturn("tenant@example.com");
        when(profiles.findByUserIdAndUser_Role_Code(7L, "TENANT")).thenReturn(Optional.empty());
        var request =
                new AdminTenantProfileUpdateRequest(
                        "tenant@example.com", "Tenant", null, null, null, null, null, null, null);

        assertThrows(TenantProfileNotFoundException.class, () -> service().update(7L, request));
    }
}
