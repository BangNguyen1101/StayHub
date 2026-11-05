package com.quanlynhatro.maintenance.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.domain.RoleEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.contract.api.ContractStatus;
import com.quanlynhatro.contract.domain.ContractEntity;
import com.quanlynhatro.contract.repository.ContractOccupantRepository;
import com.quanlynhatro.contract.repository.ContractRepository;
import com.quanlynhatro.maintenance.api.*;
import com.quanlynhatro.maintenance.domain.MaintenanceRequestEntity;
import com.quanlynhatro.maintenance.repository.*;
import com.quanlynhatro.notification.service.NotificationService;
import com.quanlynhatro.room.domain.RoomEntity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MaintenanceServiceTest {
    @Mock MaintenanceRequestRepository requests;
    @Mock MaintenanceRequestHistoryRepository histories;
    @Mock AppUserRepository users;
    @Mock ContractRepository contracts;
    @Mock ContractOccupantRepository occupants;
    @Mock NotificationService notifications;
    @InjectMocks MaintenanceServiceImpl service;

    @Test
    void tenantCreateUsesTheAssignedContractAndRecordsHistory() {
        AppUserEntity tenant = user(2L, "TENANT");
        RoomEntity room = mock(RoomEntity.class);
        ContractEntity contract = mock(ContractEntity.class);
        when(users.findByIdAndRole_Code(2L, "TENANT")).thenReturn(Optional.of(tenant));
        when(contract.getStartDate()).thenReturn(LocalDate.now().minusDays(2));
        when(contract.getEndDate()).thenReturn(LocalDate.now().plusDays(20));
        when(contract.getRoom()).thenReturn(room);
        when(contract.getId()).thenReturn(4L);
        when(contracts.findByTenantUser_Id(2L)).thenReturn(List.of(contract));
        when(requests.existsByRequestCodeIgnoreCase(anyString())).thenReturn(false);
        when(requests.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response =
                service.createForTenant(
                        new MaintenanceCreateRequest(
                                MaintenanceIssueType.WATER,
                                "Rò rỉ nước",
                                "Vòi nước bị rò",
                                MaintenancePriority.HIGH),
                        2L);

        assertEquals(MaintenanceStatus.OPEN, response.status());
        assertEquals(4L, response.contractId());
        verify(histories).save(any());
        verify(notifications)
                .notifyAdmins(
                        eq("MAINTENANCE_REQUEST"),
                        contains("Yêu cầu bảo trì mới"),
                        contains("MR-"),
                        eq("MAINTENANCE_REQUEST"),
                        isNull());
    }

    @Test
    void tenantWithMultipleContractsUsesTheCurrentOpenContract() {
        AppUserEntity tenant = user(2L, "TENANT");
        ContractEntity first = mock(ContractEntity.class), second = mock(ContractEntity.class);
        when(users.findByIdAndRole_Code(2L, "TENANT")).thenReturn(Optional.of(tenant));
        when(first.getId()).thenReturn(10L);
        when(first.getStartDate()).thenReturn(LocalDate.now().minusDays(10));
        when(first.getEndDate()).thenReturn(LocalDate.now().minusDays(1));
        when(first.getStatus()).thenReturn(ContractStatus.EXPIRED);
        when(second.getId()).thenReturn(11L);
        when(second.getStartDate()).thenReturn(LocalDate.now().minusDays(1));
        when(second.getEndDate()).thenReturn(LocalDate.now().plusDays(10));
        when(second.getStatus()).thenReturn(ContractStatus.PENDING);
        when(second.getRoom()).thenReturn(mock(RoomEntity.class));
        when(contracts.findByTenantUser_Id(2L)).thenReturn(List.of(first, second));
        when(requests.existsByRequestCodeIgnoreCase(anyString())).thenReturn(false);
        when(requests.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response =
                service.createForTenant(
                        new MaintenanceCreateRequest(
                                MaintenanceIssueType.OTHER,
                                "Lỗi",
                                "Mô tả",
                                MaintenancePriority.NORMAL),
                        2L);

        assertEquals(11L, response.contractId());
    }

    @Test
    void coOccupantCanCreateMaintenanceForTheAssignedContract() {
        AppUserEntity tenant = user(2L, "TENANT");
        RoomEntity room = mock(RoomEntity.class);
        ContractEntity contract = mock(ContractEntity.class);
        when(users.findByIdAndRole_Code(2L, "TENANT")).thenReturn(Optional.of(tenant));
        when(contract.getStartDate()).thenReturn(LocalDate.now().minusDays(2));
        when(contract.getEndDate()).thenReturn(LocalDate.now().plusDays(20));
        when(contract.getRoom()).thenReturn(room);
        when(contract.getId()).thenReturn(7L);
        when(contracts.findByTenantUser_Id(2L)).thenReturn(List.of());
        when(occupants.findContractsByUserId(eq(2L))).thenReturn(List.of(contract));
        when(requests.existsByRequestCodeIgnoreCase(anyString())).thenReturn(false);
        when(requests.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response =
                service.createForTenant(
                        new MaintenanceCreateRequest(
                                MaintenanceIssueType.EQUIPMENT,
                                "Điều hòa không mát",
                                "Đã thử bật lại nhưng không chạy",
                                MaintenancePriority.HIGH),
                        2L);

        assertEquals(7L, response.contractId());
        verify(requests).save(any());
    }

    @Test
    void invalidTransitionAndMissingRejectionReasonAreBlocked() {
        AppUserEntity admin = user(1L, "ADMIN");
        MaintenanceRequestEntity request = mock(MaintenanceRequestEntity.class);
        when(users.findByIdAndRole_Code(1L, "ADMIN")).thenReturn(Optional.of(admin));
        when(requests.findByIdForUpdate(9L)).thenReturn(Optional.of(request));
        when(request.getStatus()).thenReturn(MaintenanceStatus.OPEN);

        assertThrows(
                MaintenanceConflictException.class,
                () ->
                        service.updateForAdmin(
                                9L,
                                new MaintenanceAdminUpdateRequest(
                                        MaintenanceStatus.RESOLVED,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null),
                                1L));
        assertThrows(
                MaintenanceConflictException.class,
                () ->
                        service.updateForAdmin(
                                9L,
                                new MaintenanceAdminUpdateRequest(
                                        MaintenanceStatus.REJECTED,
                                        null,
                                        null,
                                        null,
                                        null,
                                        "",
                                        null),
                                1L));
        verify(histories, never()).save(any());
    }

    @Test
    void tenantCannotReadAnotherTenantsRequest() {
        AppUserEntity tenant = user(2L, "TENANT");
        MaintenanceRequestEntity request = mock(MaintenanceRequestEntity.class);
        AppUserEntity owner = user(3L, "TENANT");
        when(users.findByIdAndRole_Code(2L, "TENANT")).thenReturn(Optional.of(tenant));
        when(requests.findById(9L)).thenReturn(Optional.of(request));
        when(request.getReporterUser()).thenReturn(owner);

        assertThrows(MaintenanceNotFoundException.class, () -> service.getForTenant(9L, 2L));
    }

    private AppUserEntity user(Long id, String roleCode) {
        AppUserEntity user = mock(AppUserEntity.class);
        RoleEntity role = mock(RoleEntity.class);
        when(user.getId()).thenReturn(id);
        when(user.getRole()).thenReturn(role);
        when(role.getCode()).thenReturn(roleCode);
        when(user.isActive()).thenReturn(true);
        return user;
    }
}
