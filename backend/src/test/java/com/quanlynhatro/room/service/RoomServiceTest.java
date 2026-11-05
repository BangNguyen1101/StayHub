package com.quanlynhatro.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quanlynhatro.contract.api.ContractStatus;
import com.quanlynhatro.contract.domain.ContractEntity;
import com.quanlynhatro.contract.domain.ContractOccupantEntity;
import com.quanlynhatro.contract.repository.ContractOccupantRepository;
import com.quanlynhatro.contract.repository.ContractRepository;
import com.quanlynhatro.room.api.RoomCreateRequest;
import com.quanlynhatro.room.api.RoomResponse;
import com.quanlynhatro.room.api.RoomStatus;
import com.quanlynhatro.room.api.RoomStatusUpdateRequest;
import com.quanlynhatro.room.api.RoomUpdateRequest;
import com.quanlynhatro.room.domain.RoomEntity;
import com.quanlynhatro.room.repository.RoomRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock private RoomRepository roomRepository;

    private RoomServiceImpl roomService;

    @BeforeEach
    void setUp() {
        roomService = new RoomServiceImpl(roomRepository);
    }

    @Test
    void searchNormalizesFiltersAndMapsPage() {
        RoomEntity room = room("P01", RoomStatus.VACANT);
        when(roomRepository.search("P01", "VACANT", PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(room)));

        var result = roomService.search(" P01 ", " vacant ", PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.getContent().get(0).roomCode()).isEqualTo("P01");
    }

    @Test
    void currentOccupantsReturnsEveryoneAssignedToEffectiveContracts() {
        ContractRepository contracts = org.mockito.Mockito.mock(ContractRepository.class);
        ContractOccupantRepository occupants =
                org.mockito.Mockito.mock(ContractOccupantRepository.class);
        ContractOccupantEntity first = org.mockito.Mockito.mock(ContractOccupantEntity.class);
        ContractOccupantEntity second = org.mockito.Mockito.mock(ContractOccupantEntity.class);
        ContractEntity contract = org.mockito.Mockito.mock(ContractEntity.class);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room("P01", RoomStatus.OCCUPIED)));
        when(first.getId()).thenReturn(11L);
        when(first.getFullName()).thenReturn("Tenant One");
        when(first.getContract()).thenReturn(contract);
        when(first.isPrimaryTenant()).thenReturn(true);
        when(first.getMoveInDate()).thenReturn(LocalDate.of(2026, 10, 1));
        when(second.getId()).thenReturn(12L);
        when(second.getFullName()).thenReturn("Tenant Two");
        when(second.getContract()).thenReturn(contract);
        when(second.getMoveInDate()).thenReturn(LocalDate.of(2026, 10, 2));
        when(contract.getId()).thenReturn(21L);
        when(contract.getContractCode()).thenReturn("HD-21");
        when(occupants.findCurrentByRoomId(
                        org.mockito.ArgumentMatchers.eq(1L),
                        org.mockito.ArgumentMatchers.eq(
                                Set.of(ContractStatus.ACTIVE, ContractStatus.EXPIRING)),
                        org.mockito.ArgumentMatchers.any(LocalDate.class)))
                .thenReturn(List.of(first, second));

        var result = new RoomServiceImpl(roomRepository, contracts, occupants).currentOccupants(1L);

        assertThat(result)
                .extracting(response -> response.fullName())
                .containsExactly("Tenant One", "Tenant Two");
    }

    @Test
    void createRejectsDuplicateRoomCode() {
        RoomCreateRequest request = createRequest(" p01 ");
        when(roomRepository.existsByRoomCodeIgnoreCase("P01")).thenReturn(true);

        assertThatThrownBy(() -> roomService.create(request))
                .isInstanceOf(DuplicateRoomCodeException.class);
        verify(roomRepository, never()).save(any());
    }

    @Test
    void createCreatesVacantRoom() {
        RoomCreateRequest request = createRequest(" p01 ");
        when(roomRepository.existsByRoomCodeIgnoreCase("P01")).thenReturn(false);
        RoomEntity room = room("P01", RoomStatus.VACANT);
        when(roomRepository.save(any(RoomEntity.class))).thenReturn(room);

        RoomResponse response = roomService.create(request);

        assertThat(response.roomCode()).isEqualTo("P01");
        assertThat(response.status()).isEqualTo(RoomStatus.VACANT.name());
        verify(roomRepository).save(any(RoomEntity.class));
    }

    @Test
    void updateRejectsMissingRoom() {
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.update(99L, updateRequest("P99")))
                .isInstanceOf(RoomNotFoundException.class);
    }

    @Test
    void updateRejectsDuplicateRoomCode() {
        RoomEntity room = room("P01", RoomStatus.VACANT);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomRepository.existsByRoomCodeIgnoreCaseAndIdNot("P02", 1L)).thenReturn(true);

        assertThatThrownBy(() -> roomService.update(1L, updateRequest("P02")))
                .isInstanceOf(DuplicateRoomCodeException.class);
    }

    @Test
    void updateStatusUsesPessimisticLookupAndAllowsDefinedStatuses() {
        RoomEntity room = room("P01", RoomStatus.VACANT);
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(room));

        RoomResponse response =
                roomService.updateStatus(1L, new RoomStatusUpdateRequest(RoomStatus.MAINTENANCE));

        assertThat(response.status()).isEqualTo(RoomStatus.MAINTENANCE.name());
        verify(roomRepository).findByIdForUpdate(1L);
    }

    @Test
    void updateStatusBlocksOccupiedToVacantOrInactive() {
        RoomEntity room = room("P01", RoomStatus.OCCUPIED);
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(room));

        assertThatThrownBy(
                        () ->
                                roomService.updateStatus(
                                        1L, new RoomStatusUpdateRequest(RoomStatus.VACANT)))
                .isInstanceOf(RoomStatusChangeNotAllowedException.class);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.OCCUPIED.name());
    }

    @Test
    void updateStatusBlocksInactiveWhenContractCannotBeVerified() {
        RoomEntity room = room("P01", RoomStatus.MAINTENANCE);
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(room));

        assertThatThrownBy(
                        () ->
                                roomService.updateStatus(
                                        1L, new RoomStatusUpdateRequest(RoomStatus.INACTIVE)))
                .isInstanceOf(RoomStatusChangeNotAllowedException.class);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.MAINTENANCE.name());
    }

    @Test
    void updateStatusRejectsMissingRoom() {
        when(roomRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                roomService.updateStatus(
                                        99L, new RoomStatusUpdateRequest(RoomStatus.VACANT)))
                .isInstanceOf(RoomNotFoundException.class);
    }

    private RoomEntity room(String code, RoomStatus status) {
        RoomEntity room =
                RoomEntity.create(
                        code,
                        "Room " + code,
                        1,
                        new BigDecimal("20.00"),
                        new BigDecimal("3000000.00"),
                        2,
                        true,
                        null);
        if (status != RoomStatus.VACANT) {
            room.changeStatus(status);
        }
        return room;
    }

    private RoomCreateRequest createRequest(String code) {
        return new RoomCreateRequest(
                code,
                "Room",
                1,
                new BigDecimal("20.00"),
                new BigDecimal("3000000.00"),
                2,
                true,
                null);
    }

    private RoomUpdateRequest updateRequest(String code) {
        return new RoomUpdateRequest(
                code,
                "Room",
                1,
                new BigDecimal("20.00"),
                new BigDecimal("3000000.00"),
                2,
                true,
                null);
    }
}
