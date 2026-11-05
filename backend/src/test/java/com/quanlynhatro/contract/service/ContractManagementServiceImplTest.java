package com.quanlynhatro.contract.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.domain.RoleEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.contract.api.ContractCancelRequest;
import com.quanlynhatro.contract.api.ContractStatus;
import com.quanlynhatro.contract.domain.ContractEntity;
import com.quanlynhatro.contract.domain.ContractOccupantEntity;
import com.quanlynhatro.contract.repository.ContractHistoryRepository;
import com.quanlynhatro.contract.repository.ContractOccupantRepository;
import com.quanlynhatro.contract.repository.ContractRepository;
import com.quanlynhatro.rental.repository.RentalRequestRepository;
import com.quanlynhatro.room.api.RoomStatus;
import com.quanlynhatro.room.domain.RoomEntity;
import com.quanlynhatro.room.repository.RoomRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ContractManagementServiceImplTest {
    @Mock private ContractRepository contracts;
    @Mock private ContractOccupantRepository occupants;
    @Mock private ContractHistoryRepository histories;
    @Mock private AppUserRepository users;
    @Mock private RoomRepository rooms;
    @Mock private RentalRequestRepository rentalRequests;
    @Mock private ContractDocumentService documents;

    @Test
    void activationRequiresTenantSignature() {
        AppUserEntity admin = user(1L, "ADMIN");
        AppUserEntity tenant = user(2L, "TENANT");
        RoomEntity room = room(5L, 2, true, RoomStatus.VACANT);
        ContractEntity contract = contract(10L, tenant, room, ContractStatus.PENDING);
        when(users.findByIdAndRole_Code(1L, "ADMIN")).thenReturn(Optional.of(admin));
        when(contracts.findById(10L)).thenReturn(Optional.of(contract));
        when(contracts.findByIdForUpdate(10L)).thenReturn(Optional.of(contract));
        when(rooms.findByIdForUpdate(5L)).thenReturn(Optional.of(room));
        when(documents.isContractSigned(10L)).thenReturn(false);

        assertThrows(
                ContractBusinessException.class,
                () ->
                        new ContractManagementServiceImpl(
                                        contracts,
                                        occupants,
                                        histories,
                                        users,
                                        rooms,
                                        rentalRequests,
                                        null,
                                        null,
                                        documents,
                                        new ObjectMapper().findAndRegisterModules())
                                .activate(10L, 1L));
        verify(contract, never()).markActivated(any());
        verify(histories, never()).save(any());
    }

    @Test
    void submittingContractIssuesDocumentForSigning() {
        AppUserEntity admin = user(1L, "ADMIN");
        AppUserEntity tenant = user(2L, "TENANT");
        RoomEntity room = room(5L, 2, true, RoomStatus.VACANT);
        ContractEntity contract = contract(10L, tenant, room, ContractStatus.DRAFT);
        when(users.findByIdAndRole_Code(1L, "ADMIN")).thenReturn(Optional.of(admin));
        when(contracts.findById(10L)).thenReturn(Optional.of(contract));
        when(contracts.findByIdForUpdate(10L)).thenReturn(Optional.of(contract));
        when(rooms.findByIdForUpdate(5L)).thenReturn(Optional.of(room));

        new ContractManagementServiceImpl(
                        contracts,
                        occupants,
                        histories,
                        users,
                        rooms,
                        rentalRequests,
                        null,
                        null,
                        documents,
                        new ObjectMapper().findAndRegisterModules())
                .submitPending(10L, 1L);

        verify(contract).changeStatus(ContractStatus.PENDING);
        verify(documents).issueContractDocument(contract, admin);
    }

    @Test
    void pendingContractCanBeActivatedAndRoomIsSynchronized() {
        AppUserEntity admin = user(1L, "ADMIN");
        AppUserEntity tenant = user(2L, "TENANT");
        RoomEntity room = room(5L, 2, true, RoomStatus.VACANT);
        ContractEntity contract = contract(10L, tenant, room, ContractStatus.PENDING);
        ContractOccupantEntity primary = org.mockito.Mockito.mock(ContractOccupantEntity.class);
        when(primary.getUser()).thenReturn(tenant);
        when(contracts.findById(10L)).thenReturn(Optional.of(contract));
        when(contracts.findByIdForUpdate(10L)).thenReturn(Optional.of(contract));
        when(rooms.findByIdForUpdate(5L)).thenReturn(Optional.of(room));
        when(users.findByIdAndRole_Code(1L, "ADMIN")).thenReturn(Optional.of(admin));
        when(users.findByIdForUpdate(2L)).thenReturn(Optional.of(tenant));
        when(contracts.existsByTenantUser_IdAndStatusIn(eq(2L), any())).thenReturn(false);
        when(occupants.findByContract_IdAndPrimaryTenantTrue(10L)).thenReturn(Optional.of(primary));
        when(occupants.countByContract_IdAndPrimaryTenantTrue(10L)).thenReturn(1L);
        when(occupants.countStayingByRoomIdFrom(any(), any(), any())).thenReturn(0L);
        when(occupants.countStayingByContractIdFrom(10L, LocalDate.now())).thenReturn(1L);
        when(occupants.countCurrentByRoomId(any(), any(), any())).thenReturn(1L);

        new ContractManagementServiceImpl(
                        contracts,
                        occupants,
                        histories,
                        users,
                        rooms,
                        rentalRequests,
                        new ObjectMapper().findAndRegisterModules())
                .activate(10L, 1L);

        verify(contract).markActivated(any());
        verify(room).changeStatus(RoomStatus.OCCUPIED);
        verify(histories).save(any());
    }

    @Test
    void activationRejectsWhenCapacityWouldBeExceededAndDoesNotWriteHistory() {
        AppUserEntity admin = user(1L, "ADMIN");
        AppUserEntity tenant = user(2L, "TENANT");
        RoomEntity room = room(5L, 1, true, RoomStatus.VACANT);
        ContractEntity contract = contract(10L, tenant, room, ContractStatus.PENDING);
        ContractOccupantEntity primary = org.mockito.Mockito.mock(ContractOccupantEntity.class);
        when(primary.getUser()).thenReturn(tenant);
        when(users.findByIdAndRole_Code(1L, "ADMIN")).thenReturn(Optional.of(admin));
        when(contracts.findById(10L)).thenReturn(Optional.of(contract));
        when(contracts.findByIdForUpdate(10L)).thenReturn(Optional.of(contract));
        when(rooms.findByIdForUpdate(5L)).thenReturn(Optional.of(room));
        when(users.findByIdForUpdate(2L)).thenReturn(Optional.of(tenant));
        when(contracts.existsByTenantUser_IdAndStatusIn(eq(2L), any())).thenReturn(false);
        when(occupants.findByContract_IdAndPrimaryTenantTrue(10L)).thenReturn(Optional.of(primary));
        when(occupants.countByContract_IdAndPrimaryTenantTrue(10L)).thenReturn(1L);
        when(occupants.countStayingByRoomIdFrom(
                        5L,
                        java.util.List.of(ContractStatus.ACTIVE, ContractStatus.EXPIRING),
                        LocalDate.now()))
                .thenReturn(1L);
        when(occupants.countStayingByContractIdFrom(10L, LocalDate.now())).thenReturn(1L);

        assertThrows(
                ContractBusinessException.class,
                () ->
                        new ContractManagementServiceImpl(
                                        contracts,
                                        occupants,
                                        histories,
                                        users,
                                        rooms,
                                        rentalRequests,
                                        new ObjectMapper().findAndRegisterModules())
                                .activate(10L, 1L));
        verify(contract, never()).markActivated(any());
        verify(histories, never()).save(any());
    }

    @Test
    void tenantCannotReadAnotherTenantsContract() {
        AppUserEntity tenant = user(2L, "TENANT");
        AppUserEntity otherTenant = user(3L, "TENANT");
        RoomEntity room = room(5L, 2, true, RoomStatus.OCCUPIED);
        ContractEntity contract = contract(10L, otherTenant, room, ContractStatus.ACTIVE);
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("tenant", "tenant"))
                .thenReturn(Optional.of(tenant));
        when(contracts.findById(10L)).thenReturn(Optional.of(contract));
        when(occupants.existsByContract_IdAndUser_Id(10L, 2L)).thenReturn(false);

        assertThrows(
                ContractAccessDeniedException.class,
                () ->
                        new ContractManagementServiceImpl(
                                        contracts,
                                        occupants,
                                        histories,
                                        users,
                                        rooms,
                                        rentalRequests,
                                        new ObjectMapper().findAndRegisterModules())
                                .getForTenant("tenant", 10L));
    }

    @Test
    void nonAdminCannotCancelContract() {
        AppUserEntity tenant = user(2L, "TENANT");
        when(users.findByIdAndRole_Code(2L, "ADMIN")).thenReturn(Optional.empty());

        assertThrows(
                ContractAccessDeniedException.class,
                () ->
                        new ContractManagementServiceImpl(
                                        contracts,
                                        occupants,
                                        histories,
                                        users,
                                        rooms,
                                        rentalRequests,
                                        new ObjectMapper().findAndRegisterModules())
                                .cancel(10L, new ContractCancelRequest("cancel"), 2L));
        verify(contracts, never()).findById(any());
    }

    @Test
    void cancelIsRejectedForActiveContract() {
        AppUserEntity admin = user(1L, "ADMIN");
        AppUserEntity tenant = user(2L, "TENANT");
        RoomEntity room = room(5L, 2, true, RoomStatus.OCCUPIED);
        ContractEntity contract = contract(10L, tenant, room, ContractStatus.ACTIVE);
        when(users.findByIdAndRole_Code(1L, "ADMIN")).thenReturn(Optional.of(admin));
        when(contracts.findById(10L)).thenReturn(Optional.of(contract));
        when(rooms.findByIdForUpdate(5L)).thenReturn(Optional.of(room));
        when(contracts.findByIdForUpdate(10L)).thenReturn(Optional.of(contract));

        assertThrows(
                ContractBusinessException.class,
                () ->
                        new ContractManagementServiceImpl(
                                        contracts,
                                        occupants,
                                        histories,
                                        users,
                                        rooms,
                                        rentalRequests,
                                        new ObjectMapper().findAndRegisterModules())
                                .cancel(10L, new ContractCancelRequest("cancel"), 1L));
        verify(histories, never()).save(any());
    }

    @Test
    void activationRejectsTenantWithAnotherEffectiveContract() {
        AppUserEntity admin = user(1L, "ADMIN");
        AppUserEntity tenant = user(2L, "TENANT");
        RoomEntity room = room(5L, 2, true, RoomStatus.VACANT);
        ContractEntity contract = contract(10L, tenant, room, ContractStatus.PENDING);
        when(users.findByIdAndRole_Code(1L, "ADMIN")).thenReturn(Optional.of(admin));
        when(users.findByIdForUpdate(2L)).thenReturn(Optional.of(tenant));
        when(contracts.existsByTenantUser_IdAndStatusIn(eq(2L), any())).thenReturn(true);
        when(contracts.findById(10L)).thenReturn(Optional.of(contract));
        when(contracts.findByIdForUpdate(10L)).thenReturn(Optional.of(contract));
        when(rooms.findByIdForUpdate(5L)).thenReturn(Optional.of(room));

        assertThrows(ContractBusinessException.class, () -> service().activate(10L, 1L));
        verify(contract, never()).markActivated(any());
        verify(histories, never()).save(any());
    }

    @Test
    void expiringContractCanBeRenewedAndTerminated() {
        AppUserEntity admin = user(1L, "ADMIN");
        AppUserEntity tenant = user(2L, "TENANT");
        RoomEntity room = room(5L, 2, true, RoomStatus.OCCUPIED);
        ContractEntity contract = contract(10L, tenant, room, ContractStatus.EXPIRING);
        when(users.findByIdAndRole_Code(1L, "ADMIN")).thenReturn(Optional.of(admin));
        when(contracts.findById(10L)).thenReturn(Optional.of(contract));
        when(contracts.findByIdForUpdate(10L)).thenReturn(Optional.of(contract));
        when(rooms.findByIdForUpdate(5L)).thenReturn(Optional.of(room));
        when(occupants.countCurrentByRoomId(
                        5L,
                        java.util.List.of(ContractStatus.ACTIVE, ContractStatus.EXPIRING),
                        LocalDate.now()))
                .thenReturn(0L);

        service()
                .renew(
                        10L,
                        new com.quanlynhatro.contract.api.ContractRenewRequest(
                                LocalDate.now().plusMonths(3)),
                        1L);
        verify(contract).renewUntil(any());
        service()
                .terminate(
                        10L,
                        new com.quanlynhatro.contract.api.ContractTerminateRequest(
                                LocalDate.now(), "end"),
                        1L);
        verify(contract).markTerminated(eq(LocalDate.now()), any());
        verify(room).changeStatus(RoomStatus.VACANT);
        verify(histories, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void schedulerMarksActiveContractExpiringAtThirtyDayHorizon() {
        AppUserEntity admin = user(1L, "ADMIN");
        AppUserEntity tenant = user(2L, "TENANT");
        RoomEntity room = room(5L, 2, true, RoomStatus.OCCUPIED);
        ContractEntity candidate = contract(10L, tenant, room, ContractStatus.ACTIVE);
        when(candidate.getEndDate()).thenReturn(LocalDate.of(2026, 11, 18));
        when(users.findFirstByRole_CodeAndActiveTrueOrderByIdAsc("ADMIN"))
                .thenReturn(Optional.of(admin));
        when(contracts.findByStatusInAndEndDateLessThanEqualOrderByIdAsc(
                        any(), eq(LocalDate.of(2026, 10, 19).plusDays(30))))
                .thenReturn(java.util.List.of(candidate));
        when(contracts.findById(10L)).thenReturn(Optional.of(candidate));
        when(rooms.findByIdForUpdate(5L)).thenReturn(Optional.of(room));
        when(contracts.findByIdForUpdate(10L)).thenReturn(Optional.of(candidate));

        service().processScheduledExpiry(LocalDate.of(2026, 10, 19));

        verify(candidate).changeStatus(ContractStatus.EXPIRING);
        verify(histories).save(any());
        verify(candidate, never()).markExpired(any());
    }

    @Test
    void schedulerMarksPastActiveContractExpiredAndSynchronizesRoom() {
        AppUserEntity admin = user(1L, "ADMIN");
        AppUserEntity tenant = user(2L, "TENANT");
        RoomEntity room = room(5L, 2, true, RoomStatus.OCCUPIED);
        ContractEntity candidate = contract(10L, tenant, room, ContractStatus.ACTIVE);
        when(candidate.getEndDate()).thenReturn(LocalDate.of(2026, 10, 18));
        when(users.findFirstByRole_CodeAndActiveTrueOrderByIdAsc("ADMIN"))
                .thenReturn(Optional.of(admin));
        when(contracts.findByStatusInAndEndDateLessThanEqualOrderByIdAsc(any(), any()))
                .thenReturn(java.util.List.of(candidate));
        when(contracts.findById(10L)).thenReturn(Optional.of(candidate));
        when(rooms.findByIdForUpdate(5L)).thenReturn(Optional.of(room));
        when(contracts.findByIdForUpdate(10L)).thenReturn(Optional.of(candidate));
        when(occupants.countCurrentByRoomId(
                        5L,
                        java.util.List.of(ContractStatus.ACTIVE, ContractStatus.EXPIRING),
                        LocalDate.of(2026, 10, 19)))
                .thenReturn(0L);

        service().processScheduledExpiry(LocalDate.of(2026, 10, 19));

        verify(candidate).markExpired(LocalDate.of(2026, 10, 18));
        verify(room).changeStatus(RoomStatus.VACANT);
        verify(histories).save(any());
    }

    @Test
    void schedulerIsIdempotentWhenThereAreNoCandidatesOnRepeatedRuns() {
        AppUserEntity admin = user(1L, "ADMIN");
        when(users.findFirstByRole_CodeAndActiveTrueOrderByIdAsc("ADMIN"))
                .thenReturn(Optional.of(admin));
        when(contracts.findByStatusInAndEndDateLessThanEqualOrderByIdAsc(any(), any()))
                .thenReturn(java.util.List.of());

        service().processScheduledExpiry(LocalDate.of(2026, 10, 19));
        service().processScheduledExpiry(LocalDate.of(2026, 10, 19));

        verify(contracts, org.mockito.Mockito.times(2))
                .findByStatusInAndEndDateLessThanEqualOrderByIdAsc(any(), any());
        verify(histories, never()).save(any());
    }

    @Test
    void schedulerContinuesWhenOneContractCannotBeLocked() {
        AppUserEntity admin = user(1L, "ADMIN");
        AppUserEntity tenant = user(2L, "TENANT");
        RoomEntity room = room(5L, 2, true, RoomStatus.OCCUPIED);
        ContractEntity failed = contract(10L, tenant, room, ContractStatus.ACTIVE);
        ContractEntity valid = contract(11L, tenant, room, ContractStatus.ACTIVE);
        when(valid.getEndDate()).thenReturn(LocalDate.of(2026, 11, 18));
        when(users.findFirstByRole_CodeAndActiveTrueOrderByIdAsc("ADMIN"))
                .thenReturn(Optional.of(admin));
        when(contracts.findByStatusInAndEndDateLessThanEqualOrderByIdAsc(any(), any()))
                .thenReturn(java.util.List.of(failed, valid));
        when(contracts.findById(10L)).thenReturn(Optional.empty());
        when(contracts.findById(11L)).thenReturn(Optional.of(valid));
        when(rooms.findByIdForUpdate(5L)).thenReturn(Optional.of(room));
        when(contracts.findByIdForUpdate(11L)).thenReturn(Optional.of(valid));

        service().processScheduledExpiry(LocalDate.of(2026, 10, 19));

        verify(valid).changeStatus(ContractStatus.EXPIRING);
    }

    private ContractManagementServiceImpl service() {
        return new ContractManagementServiceImpl(
                contracts,
                occupants,
                histories,
                users,
                rooms,
                rentalRequests,
                new ObjectMapper().findAndRegisterModules());
    }

    private AppUserEntity user(Long id, String roleCode) {
        AppUserEntity user = org.mockito.Mockito.mock(AppUserEntity.class);
        RoleEntity role = org.mockito.Mockito.mock(RoleEntity.class);
        lenient().when(user.getId()).thenReturn(id);
        lenient().when(user.getRole()).thenReturn(role);
        lenient().when(role.getCode()).thenReturn(roleCode);
        lenient().when(user.getFullName()).thenReturn("User " + id);
        lenient().when(user.getPhoneNumber()).thenReturn("090000000" + id);
        return user;
    }

    private RoomEntity room(Long id, int maxOccupants, boolean sharing, RoomStatus status) {
        RoomEntity room = org.mockito.Mockito.mock(RoomEntity.class);
        lenient().when(room.getId()).thenReturn(id);
        lenient().when(room.getMaxOccupants()).thenReturn(maxOccupants);
        lenient().when(room.isAllowsSharing()).thenReturn(sharing);
        lenient().when(room.getStatus()).thenReturn(status.name());
        return room;
    }

    private ContractEntity contract(
            Long id, AppUserEntity tenant, RoomEntity room, ContractStatus status) {
        ContractEntity contract = org.mockito.Mockito.mock(ContractEntity.class);
        lenient().when(contract.getId()).thenReturn(id);
        lenient().when(contract.getTenantUser()).thenReturn(tenant);
        lenient().when(contract.getRoom()).thenReturn(room);
        lenient().when(contract.getStatus()).thenReturn(status);
        lenient().when(contract.getContractCode()).thenReturn("HD-" + id);
        lenient().when(contract.getStartDate()).thenReturn(LocalDate.now());
        lenient().when(contract.getEndDate()).thenReturn(LocalDate.now().plusMonths(1));
        lenient().when(contract.getMonthlyRent()).thenReturn(java.math.BigDecimal.ONE);
        lenient().when(contract.getDepositAmount()).thenReturn(java.math.BigDecimal.ONE);
        lenient().when(contract.getBillingCycleMonths()).thenReturn(1);
        lenient().when(contract.getDueDay()).thenReturn((short) 5);
        return contract;
    }
}
