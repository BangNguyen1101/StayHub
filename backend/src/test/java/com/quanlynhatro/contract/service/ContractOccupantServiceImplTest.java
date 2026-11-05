package com.quanlynhatro.contract.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.domain.RoleEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.contract.api.ContractOccupantCreateRequest;
import com.quanlynhatro.contract.api.ContractStatus;
import com.quanlynhatro.contract.domain.ContractEntity;
import com.quanlynhatro.contract.domain.ContractOccupantEntity;
import com.quanlynhatro.contract.repository.ContractHistoryRepository;
import com.quanlynhatro.contract.repository.ContractOccupantRepository;
import com.quanlynhatro.contract.repository.ContractRepository;
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
class ContractOccupantServiceImplTest {
    @Mock private ContractRepository contracts;
    @Mock private ContractOccupantRepository occupants;
    @Mock private ContractHistoryRepository histories;
    @Mock private AppUserRepository users;
    @Mock private RoomRepository rooms;

    @Test
    void addingOccupantRejectsCapacityAndDoesNotPersist() {
        AppUserEntity admin = user(1L, "ADMIN");
        AppUserEntity tenant = user(2L, "TENANT");
        RoomEntity room = room(5L, 1, true);
        ContractEntity contract = contract(10L, tenant, room);
        when(users.findByIdAndRole_Code(1L, "ADMIN")).thenReturn(Optional.of(admin));
        when(contracts.findById(10L)).thenReturn(Optional.of(contract));
        when(contracts.findByIdForUpdate(10L)).thenReturn(Optional.of(contract));
        when(rooms.findByIdForUpdate(5L)).thenReturn(Optional.of(room));
        when(occupants.countStayingByRoomIdFrom(
                        5L,
                        java.util.List.of(ContractStatus.ACTIVE, ContractStatus.EXPIRING),
                        LocalDate.now()))
                .thenReturn(1L);

        var request =
                new ContractOccupantCreateRequest(
                        null, "Guest", null, null, false, LocalDate.now());
        assertThrows(
                ContractBusinessException.class,
                () ->
                        new ContractOccupantServiceImpl(
                                        contracts,
                                        occupants,
                                        histories,
                                        users,
                                        rooms,
                                        new ObjectMapper().findAndRegisterModules())
                                .add(10L, request, 1L));
        verify(occupants, never()).save(any());
        verify(histories, never()).save(any());
    }

    @Test
    void backDatedMoveInStillCountsOccupantsWhoArriveLater() {
        AppUserEntity admin = user(1L, "ADMIN");
        AppUserEntity tenant = user(2L, "TENANT");
        RoomEntity room = room(5L, 2, true);
        ContractEntity contract = contract(10L, tenant, room);
        LocalDate backDated = LocalDate.now().minusDays(15);
        when(users.findByIdAndRole_Code(1L, "ADMIN")).thenReturn(Optional.of(admin));
        when(contracts.findById(10L)).thenReturn(Optional.of(contract));
        when(contracts.findByIdForUpdate(10L)).thenReturn(Optional.of(contract));
        when(rooms.findByIdForUpdate(5L)).thenReturn(Optional.of(room));
        // Two occupants already stay from the back-dated day on (one moved in later).
        when(occupants.countStayingByContractIdFrom(10L, backDated)).thenReturn(2L);

        var request = new ContractOccupantCreateRequest(null, "Third", null, null, false, backDated);
        assertThrows(ContractBusinessException.class, () -> service().add(10L, request, 1L));
        verify(occupants, never()).save(any());
    }

    @Test
    void sameTenantCannotBeAddedTwiceToOneRoom() {
        AppUserEntity admin = user(1L, "ADMIN");
        AppUserEntity tenant = user(2L, "TENANT");
        RoomEntity room = room(5L, 3, true);
        ContractEntity contract = contract(10L, tenant, room);
        ContractOccupantEntity primary = occupant(20L, tenant, "Tenant", "0900000000");
        when(users.findByIdAndRole_Code(1L, "ADMIN")).thenReturn(Optional.of(admin));
        when(users.findByIdAndRole_Code(2L, "TENANT")).thenReturn(Optional.of(tenant));
        when(contracts.findById(10L)).thenReturn(Optional.of(contract));
        when(contracts.findByIdForUpdate(10L)).thenReturn(Optional.of(contract));
        when(rooms.findByIdForUpdate(5L)).thenReturn(Optional.of(room));
        when(occupants.findByContract_IdOrderByMoveInDateAsc(10L))
                .thenReturn(java.util.List.of(primary));

        var request =
                new ContractOccupantCreateRequest(
                        2L, "Tenant", "0900000000", null, false, LocalDate.now());
        assertThrows(ContractBusinessException.class, () -> service().add(10L, request, 1L));
        verify(occupants, never()).save(any());
    }

    @Test
    void samePersonWithoutAccountCannotBeAddedTwice() {
        AppUserEntity admin = user(1L, "ADMIN");
        AppUserEntity tenant = user(2L, "TENANT");
        RoomEntity room = room(5L, 3, true);
        ContractEntity contract = contract(10L, tenant, room);
        ContractOccupantEntity guest = occupant(21L, null, "Chu Tien Dung", "078743832");
        when(users.findByIdAndRole_Code(1L, "ADMIN")).thenReturn(Optional.of(admin));
        when(contracts.findById(10L)).thenReturn(Optional.of(contract));
        when(contracts.findByIdForUpdate(10L)).thenReturn(Optional.of(contract));
        when(rooms.findByIdForUpdate(5L)).thenReturn(Optional.of(room));
        when(occupants.findByContract_IdOrderByMoveInDateAsc(10L))
                .thenReturn(java.util.List.of(guest));

        var request =
                new ContractOccupantCreateRequest(
                        null, " chu tien dung ", "078743832", null, false, LocalDate.now());
        assertThrows(ContractBusinessException.class, () -> service().add(10L, request, 1L));
        verify(occupants, never()).save(any());
    }

    @Test
    void coOccupantCanBeRemovedAndRemovalIsRecorded() {
        AppUserEntity admin = user(1L, "ADMIN");
        AppUserEntity tenant = user(2L, "TENANT");
        RoomEntity room = room(5L, 2, true);
        ContractEntity contract = contract(10L, tenant, room);
        ContractOccupantEntity guest = occupant(21L, null, "Guest", null);
        lenient().when(guest.getContract()).thenReturn(contract);
        when(occupants.findById(21L)).thenReturn(Optional.of(guest));
        when(occupants.findByIdForUpdate(21L)).thenReturn(Optional.of(guest));
        when(users.findByIdAndRole_Code(1L, "ADMIN")).thenReturn(Optional.of(admin));
        when(contracts.findById(10L)).thenReturn(Optional.of(contract));
        when(contracts.findByIdForUpdate(10L)).thenReturn(Optional.of(contract));
        when(rooms.findByIdForUpdate(5L)).thenReturn(Optional.of(room));
        when(occupants.countCurrentByRoomId(any(), any(), any())).thenReturn(1L);

        service().remove(21L, 1L);

        verify(occupants).delete(guest);
        verify(room).changeStatus(RoomStatus.OCCUPIED);
        verify(histories).save(any());
    }

    @Test
    void primaryTenantCannotBeRemoved() {
        AppUserEntity admin = user(1L, "ADMIN");
        AppUserEntity tenant = user(2L, "TENANT");
        RoomEntity room = room(5L, 2, true);
        ContractEntity contract = contract(10L, tenant, room);
        ContractOccupantEntity primary = occupant(20L, tenant, "Tenant", null);
        lenient().when(primary.getContract()).thenReturn(contract);
        when(primary.isPrimaryTenant()).thenReturn(true);
        when(occupants.findById(20L)).thenReturn(Optional.of(primary));
        when(occupants.findByIdForUpdate(20L)).thenReturn(Optional.of(primary));
        when(users.findByIdAndRole_Code(1L, "ADMIN")).thenReturn(Optional.of(admin));
        when(contracts.findById(10L)).thenReturn(Optional.of(contract));
        when(contracts.findByIdForUpdate(10L)).thenReturn(Optional.of(contract));
        when(rooms.findByIdForUpdate(5L)).thenReturn(Optional.of(room));

        assertThrows(ContractBusinessException.class, () -> service().remove(20L, 1L));
        verify(occupants, never()).delete(any());
        verify(histories, never()).save(any());
    }

    private ContractOccupantServiceImpl service() {
        return new ContractOccupantServiceImpl(
                contracts,
                occupants,
                histories,
                users,
                rooms,
                new ObjectMapper().findAndRegisterModules());
    }

    private ContractOccupantEntity occupant(
            Long id, AppUserEntity user, String fullName, String phoneNumber) {
        ContractOccupantEntity occupant = org.mockito.Mockito.mock(ContractOccupantEntity.class);
        lenient().when(occupant.getId()).thenReturn(id);
        lenient().when(occupant.getUser()).thenReturn(user);
        lenient().when(occupant.getFullName()).thenReturn(fullName);
        lenient().when(occupant.getPhoneNumber()).thenReturn(phoneNumber);
        return occupant;
    }

    @Test
    void primaryOccupantMustMatchContractTenant() {
        AppUserEntity admin = user(1L, "ADMIN");
        AppUserEntity tenant = user(2L, "TENANT");
        AppUserEntity differentTenant = user(3L, "TENANT");
        RoomEntity room = room(5L, 2, true);
        ContractEntity contract = contract(10L, tenant, room);
        when(users.findByIdAndRole_Code(1L, "ADMIN")).thenReturn(Optional.of(admin));
        when(users.findByIdAndRole_Code(3L, "TENANT")).thenReturn(Optional.of(differentTenant));
        when(contracts.findById(10L)).thenReturn(Optional.of(contract));
        when(contracts.findByIdForUpdate(10L)).thenReturn(Optional.of(contract));
        when(rooms.findByIdForUpdate(5L)).thenReturn(Optional.of(room));

        var request =
                new ContractOccupantCreateRequest(3L, "Other", null, null, true, LocalDate.now());
        assertThrows(
                ContractBusinessException.class,
                () ->
                        new ContractOccupantServiceImpl(
                                        contracts,
                                        occupants,
                                        histories,
                                        users,
                                        rooms,
                                        new ObjectMapper().findAndRegisterModules())
                                .add(10L, request, 1L));
        verify(occupants, never()).save(any());
    }

    @Test
    void nonAdminCannotManageOccupants() {
        when(users.findByIdAndRole_Code(2L, "ADMIN")).thenReturn(Optional.empty());
        var request =
                new ContractOccupantCreateRequest(
                        null, "Guest", null, null, false, LocalDate.now());

        assertThrows(
                ContractAccessDeniedException.class,
                () ->
                        new ContractOccupantServiceImpl(
                                        contracts,
                                        occupants,
                                        histories,
                                        users,
                                        rooms,
                                        new ObjectMapper().findAndRegisterModules())
                                .add(10L, request, 2L));
        verify(contracts, never()).findById(any());
    }

    @Test
    void primaryOccupantCannotMoveOutSeparately() {
        AppUserEntity admin = user(1L, "ADMIN");
        AppUserEntity tenant = user(2L, "TENANT");
        RoomEntity room = room(5L, 2, true);
        ContractEntity contract = contract(10L, tenant, room);
        ContractOccupantEntity primary = org.mockito.Mockito.mock(ContractOccupantEntity.class);
        when(primary.getContract()).thenReturn(contract);
        when(primary.isPrimaryTenant()).thenReturn(true);
        when(primary.getFullName()).thenReturn("Primary");
        when(primary.getMoveInDate()).thenReturn(LocalDate.now());
        when(occupants.findById(20L)).thenReturn(Optional.of(primary));
        when(users.findByIdAndRole_Code(1L, "ADMIN")).thenReturn(Optional.of(admin));
        when(contracts.findById(10L)).thenReturn(Optional.of(contract));
        when(contracts.findByIdForUpdate(10L)).thenReturn(Optional.of(contract));
        when(rooms.findByIdForUpdate(5L)).thenReturn(Optional.of(room));
        when(occupants.findByIdForUpdate(20L)).thenReturn(Optional.of(primary));

        assertThrows(
                ContractBusinessException.class,
                () ->
                        new ContractOccupantServiceImpl(
                                        contracts,
                                        occupants,
                                        histories,
                                        users,
                                        rooms,
                                        new ObjectMapper().findAndRegisterModules())
                                .moveOut(20L, LocalDate.now(), 1L));
        verify(occupants, never()).save(any());
    }

    private AppUserEntity user(Long id, String roleCode) {
        AppUserEntity user = org.mockito.Mockito.mock(AppUserEntity.class);
        RoleEntity role = org.mockito.Mockito.mock(RoleEntity.class);
        lenient().when(user.getId()).thenReturn(id);
        lenient().when(user.getRole()).thenReturn(role);
        lenient().when(role.getCode()).thenReturn(roleCode);
        return user;
    }

    private RoomEntity room(Long id, int maxOccupants, boolean sharing) {
        RoomEntity room = org.mockito.Mockito.mock(RoomEntity.class);
        lenient().when(room.getId()).thenReturn(id);
        lenient().when(room.getMaxOccupants()).thenReturn(maxOccupants);
        lenient().when(room.isAllowsSharing()).thenReturn(sharing);
        lenient().when(room.getStatus()).thenReturn(RoomStatus.OCCUPIED.name());
        return room;
    }

    private ContractEntity contract(Long id, AppUserEntity tenant, RoomEntity room) {
        ContractEntity contract = org.mockito.Mockito.mock(ContractEntity.class);
        lenient().when(contract.getId()).thenReturn(id);
        lenient().when(contract.getTenantUser()).thenReturn(tenant);
        lenient().when(contract.getRoom()).thenReturn(room);
        lenient().when(contract.getStatus()).thenReturn(ContractStatus.ACTIVE);
        return contract;
    }
}
