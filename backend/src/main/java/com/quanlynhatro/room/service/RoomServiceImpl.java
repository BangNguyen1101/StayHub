package com.quanlynhatro.room.service;

import com.quanlynhatro.contract.api.ContractStatus;
import com.quanlynhatro.contract.repository.ContractOccupantRepository;
import com.quanlynhatro.contract.repository.ContractRepository;
import com.quanlynhatro.room.api.RoomCreateRequest;
import com.quanlynhatro.room.api.RoomOccupantResponse;
import com.quanlynhatro.room.api.RoomResponse;
import com.quanlynhatro.room.api.RoomStatus;
import com.quanlynhatro.room.api.RoomStatusUpdateRequest;
import com.quanlynhatro.room.api.RoomUpdateRequest;
import com.quanlynhatro.room.domain.RoomEntity;
import com.quanlynhatro.room.repository.RoomRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final ContractRepository contractRepository;
    private final ContractOccupantRepository contractOccupantRepository;

    public RoomServiceImpl(RoomRepository roomRepository) {
        this(roomRepository, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public RoomServiceImpl(
            RoomRepository roomRepository,
            ContractRepository contractRepository,
            ContractOccupantRepository contractOccupantRepository) {
        this.roomRepository = roomRepository;
        this.contractRepository = contractRepository;
        this.contractOccupantRepository = contractOccupantRepository;
    }

    @Override
    public Page<RoomResponse> search(String keyword, String status, Pageable pageable) {
        String normalizedKeyword = normalize(keyword);
        String normalizedStatus = normalizeStatus(status);
        return roomRepository
                .search(normalizedKeyword, normalizedStatus, pageable)
                .map(this::toResponse);
    }

    @Override
    public Page<RoomResponse> available(Pageable pageable) {
        return roomRepository
                .findByStatus(RoomStatus.VACANT.name(), pageable)
                .map(this::toResponse);
    }

    @Override
    public RoomResponse getById(Long id) {
        return toResponse(findRoom(id));
    }

    @Override
    public List<RoomOccupantResponse> currentOccupants(Long id) {
        findRoom(id);
        if (contractOccupantRepository == null) {
            return List.of();
        }
        return contractOccupantRepository
                .findCurrentByRoomId(
                        id, Set.of(ContractStatus.ACTIVE, ContractStatus.EXPIRING), LocalDate.now())
                .stream()
                .map(
                        occupant ->
                                new RoomOccupantResponse(
                                        occupant.getId(),
                                        occupant.getUser() == null
                                                ? null
                                                : occupant.getUser().getId(),
                                        occupant.getFullName(),
                                        occupant.getPhoneNumber(),
                                        occupant.getIdentityDocumentNumber(),
                                        occupant.getContract().getId(),
                                        occupant.getContract().getContractCode(),
                                        occupant.isPrimaryTenant(),
                                        occupant.getMoveInDate(),
                                        occupant.getMoveOutDate()))
                .toList();
    }

    @Override
    @Transactional
    public RoomResponse create(RoomCreateRequest request) {
        String roomCode = normalizeRequired(request.roomCode());
        if (roomRepository.existsByRoomCodeIgnoreCase(roomCode)) {
            throw new DuplicateRoomCodeException(roomCode);
        }

        RoomEntity room =
                RoomEntity.create(
                        roomCode,
                        request.roomName(),
                        request.floorNumber(),
                        request.areaM2(),
                        request.baseRent(),
                        request.maxOccupants(),
                        request.allowsSharing(),
                        request.notes());
        return toResponse(roomRepository.save(room));
    }

    @Override
    @Transactional
    public RoomResponse update(Long id, RoomUpdateRequest request) {
        RoomEntity room = findRoom(id);
        String roomCode = normalizeRequired(request.roomCode());
        if (roomRepository.existsByRoomCodeIgnoreCaseAndIdNot(roomCode, id)) {
            throw new DuplicateRoomCodeException(roomCode);
        }

        room.updateDetails(
                roomCode,
                request.roomName(),
                request.floorNumber(),
                request.areaM2(),
                request.baseRent(),
                request.maxOccupants(),
                request.allowsSharing(),
                request.notes());
        return toResponse(room);
    }

    @Override
    @Transactional
    public RoomResponse updateStatus(Long id, RoomStatusUpdateRequest request) {
        RoomEntity room =
                roomRepository
                        .findByIdForUpdate(id)
                        .orElseThrow(() -> new RoomNotFoundException(id));
        RoomStatus targetStatus = request.status();
        String currentStatus = room.getStatus();

        if (targetStatus == RoomStatus.VACANT || targetStatus == RoomStatus.INACTIVE) {
            if (contractRepository == null
                    || contractRepository.existsByRoom_IdAndStatusIn(
                            id, java.util.Set.of(ContractStatus.ACTIVE, ContractStatus.EXPIRING))) {
                throw new RoomStatusChangeNotAllowedException(currentStatus, targetStatus.name());
            }
        }

        room.changeStatus(targetStatus);
        return toResponse(room);
    }

    private RoomEntity findRoom(Long id) {
        return roomRepository.findById(id).orElseThrow(() -> new RoomNotFoundException(id));
    }

    private RoomResponse toResponse(RoomEntity room) {
        return new RoomResponse(
                room.getId(),
                room.getRoomCode(),
                room.getRoomName(),
                room.getFloorNumber(),
                room.getAreaM2(),
                room.getBaseRent(),
                room.getMaxOccupants(),
                room.isAllowsSharing(),
                room.getStatus(),
                room.getNotes(),
                room.getCreatedAt(),
                room.getUpdatedAt());
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeRequired(String value) {
        return normalize(value).toUpperCase(Locale.ROOT);
    }

    private String normalizeStatus(String status) {
        String normalized = normalize(status);
        return normalized == null || normalized.isEmpty()
                ? normalized
                : normalized.toUpperCase(Locale.ROOT);
    }
}
