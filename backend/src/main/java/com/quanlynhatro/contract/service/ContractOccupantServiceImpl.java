package com.quanlynhatro.contract.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.contract.api.ContractOccupantCreateRequest;
import com.quanlynhatro.contract.api.ContractOccupantResponse;
import com.quanlynhatro.contract.api.ContractOccupantUpdateRequest;
import com.quanlynhatro.contract.api.ContractStatus;
import com.quanlynhatro.contract.domain.ContractEntity;
import com.quanlynhatro.contract.domain.ContractHistoryEntity;
import com.quanlynhatro.contract.domain.ContractOccupantEntity;
import com.quanlynhatro.contract.repository.ContractHistoryRepository;
import com.quanlynhatro.contract.repository.ContractOccupantRepository;
import com.quanlynhatro.contract.repository.ContractRepository;
import com.quanlynhatro.room.api.RoomStatus;
import com.quanlynhatro.room.domain.RoomEntity;
import com.quanlynhatro.room.repository.RoomRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ContractOccupantServiceImpl implements ContractOccupantService {
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String TENANT_ROLE = "TENANT";
    private static final List<ContractStatus> EFFECTIVE_STATUSES =
            List.of(ContractStatus.ACTIVE, ContractStatus.EXPIRING);

    private final ContractRepository contracts;
    private final ContractOccupantRepository occupants;
    private final ContractHistoryRepository histories;
    private final AppUserRepository users;
    private final RoomRepository rooms;
    private final ObjectMapper objectMapper;

    public ContractOccupantServiceImpl(
            ContractRepository contracts,
            ContractOccupantRepository occupants,
            ContractHistoryRepository histories,
            AppUserRepository users,
            RoomRepository rooms,
            ObjectMapper objectMapper) {
        this.contracts = contracts;
        this.occupants = occupants;
        this.histories = histories;
        this.users = users;
        this.rooms = rooms;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ContractOccupantResponse> list(Long contractId) {
        if (!contracts.existsById(contractId)) {
            throw new ContractNotFoundException(contractId);
        }
        return occupants.findByContract_IdOrderByMoveInDateAsc(contractId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ContractOccupantResponse add(
            Long contractId, ContractOccupantCreateRequest request, Long adminUserId) {
        AppUserEntity admin = requireAdmin(adminUserId);
        LockedOccupantContext context = lockContract(contractId);
        ContractEntity contract = context.contract();
        RoomEntity room = context.room();
        ensureEditable(contract);

        AppUserEntity user = resolveOccupantUser(request.userId());
        if (request.primaryTenant()) {
            if (user == null || !user.getId().equals(contract.getTenantUser().getId())) {
                throw new ContractBusinessException("Primary occupant must match TenantUserId");
            }
            if (occupants.countByContract_IdAndPrimaryTenantTrue(contractId) > 0) {
                throw new ContractBusinessException("Contract already has a primary tenant");
            }
        }
        validateDate(request.moveInDate(), null);
        String fullName = request.fullName().trim();
        String phoneNumber = normalize(request.phoneNumber());
        String identityNumber = normalize(request.identityDocumentNumber());
        ensureNotDuplicateInContract(
                contractId, null, user, fullName, phoneNumber, identityNumber);
        if (!request.primaryTenant()
                && user != null
                && occupants.existsStayingByUserIdFrom(
                        user.getId(), EFFECTIVE_STATUSES, request.moveInDate())) {
            throw new ContractBusinessException("Người thuê này đang ở một phòng khác");
        }
        ensureCapacityForNewOccupant(contract, room, request.moveInDate());

        ContractOccupantEntity occupant =
                ContractOccupantEntity.create(
                        contract,
                        user,
                        fullName,
                        phoneNumber,
                        identityNumber,
                        request.primaryTenant(),
                        request.moveInDate());
        occupants.save(occupant);
        synchronizeRoom(room);
        writeHistory(contract, "OCCUPANT_ADDED", null, snapshot(occupant), admin);
        return toResponse(occupant);
    }

    @Override
    @Transactional
    public ContractOccupantResponse update(
            Long occupantId, ContractOccupantUpdateRequest request, Long adminUserId) {
        AppUserEntity admin = requireAdmin(adminUserId);
        ContractOccupantEntity initial =
                occupants
                        .findById(occupantId)
                        .orElseThrow(
                                () ->
                                        new ContractBusinessException(
                                                "Contract occupant not found: " + occupantId));
        LockedOccupantContext context = lockContract(initial.getContract().getId());
        ContractOccupantEntity occupant =
                occupants
                        .findByIdForUpdate(occupantId)
                        .orElseThrow(
                                () ->
                                        new ContractBusinessException(
                                                "Contract occupant not found: " + occupantId));
        ContractEntity contract = context.contract();
        ensureEditable(contract);
        validateDate(request.moveInDate(), request.moveOutDate());
        if (occupant.isPrimaryTenant() && request.moveOutDate() != null) {
            throw new ContractBusinessException("The primary tenant cannot move out separately");
        }

        AppUserEntity selectedUser =
                request.userId() == null
                        ? occupant.getUser()
                        : resolveOccupantUser(request.userId());
        if (!occupant.isPrimaryTenant()
                && selectedUser != null
                && (occupant.getUser() == null
                        || !selectedUser.getId().equals(occupant.getUser().getId()))
                && occupants.existsCurrentByUserId(
                        selectedUser.getId(), EFFECTIVE_STATUSES, LocalDate.now())) {
            throw new ContractBusinessException("Người thuê này đang ở một phòng khác");
        }
        if (occupant.isPrimaryTenant()
                && selectedUser != null
                && !selectedUser.getId().equals(contract.getTenantUser().getId())) {
            throw new ContractBusinessException("The primary occupant must match TenantUserId");
        }
        String fullName = request.fullName().trim();
        String phoneNumber = normalize(request.phoneNumber());
        String identityNumber = normalize(request.identityDocumentNumber());
        // Moving someone out must always be possible, even when legacy data is already
        // duplicated or over capacity; only an occupant who keeps staying is re-validated.
        if (isStaying(request.moveOutDate(), LocalDate.now())) {
            ensureNotDuplicateInContract(
                    contract.getId(),
                    occupant.getId(),
                    selectedUser,
                    fullName,
                    phoneNumber,
                    identityNumber);
        }
        boolean extendsStay =
                request.moveInDate().isBefore(occupant.getMoveInDate())
                        || (occupant.getMoveOutDate() != null
                                && (request.moveOutDate() == null
                                        || request.moveOutDate()
                                                .isAfter(occupant.getMoveOutDate())));

        String before = snapshot(occupant);
        occupant.update(
                selectedUser,
                fullName,
                phoneNumber,
                identityNumber,
                request.moveInDate(),
                request.moveOutDate());
        if (extendsStay) {
            ensureCapacityAfterChange(contract, context.room(), request.moveInDate());
        }
        synchronizeRoom(context.room());
        writeHistory(contract, "OCCUPANT_UPDATED", before, snapshot(occupant), admin);
        return toResponse(occupant);
    }

    @Override
    @Transactional
    public ContractOccupantResponse moveOut(
            Long occupantId, LocalDate moveOutDate, Long adminUserId) {
        ContractOccupantEntity occupant =
                occupants
                        .findById(occupantId)
                        .orElseThrow(
                                () ->
                                        new ContractBusinessException(
                                                "Contract occupant not found: " + occupantId));
        ContractOccupantUpdateRequest request =
                new ContractOccupantUpdateRequest(
                        occupant.getUser() == null ? null : occupant.getUser().getId(),
                        occupant.getFullName(),
                        occupant.getPhoneNumber(),
                        occupant.getIdentityDocumentNumber(),
                        occupant.getMoveInDate(),
                        moveOutDate);
        return update(occupantId, request, adminUserId);
    }

    /**
     * Removes a co-occupant from the room entirely (e.g. added by mistake). The removal is kept in
     * the contract history; the primary tenant can only leave by ending the contract.
     */
    @Override
    @Transactional
    public void remove(Long occupantId, Long adminUserId) {
        AppUserEntity admin = requireAdmin(adminUserId);
        ContractOccupantEntity initial =
                occupants
                        .findById(occupantId)
                        .orElseThrow(
                                () ->
                                        new ContractBusinessException(
                                                "Contract occupant not found: " + occupantId));
        LockedOccupantContext context = lockContract(initial.getContract().getId());
        ContractOccupantEntity occupant =
                occupants
                        .findByIdForUpdate(occupantId)
                        .orElseThrow(
                                () ->
                                        new ContractBusinessException(
                                                "Contract occupant not found: " + occupantId));
        ContractEntity contract = context.contract();
        ensureEditable(contract);
        if (occupant.isPrimaryTenant()) {
            throw new ContractBusinessException(
                    "Không thể xoá người thuê chính, hãy kết thúc hợp đồng thay vào đó");
        }

        String before = snapshot(occupant);
        occupants.delete(occupant);
        occupants.flush();
        synchronizeRoom(context.room());
        writeHistory(contract, "OCCUPANT_REMOVED", before, null, admin);
    }

    private AppUserEntity requireAdmin(Long userId) {
        return users.findByIdAndRole_Code(userId, ADMIN_ROLE)
                .orElseThrow(ContractAccessDeniedException::new);
    }

    private AppUserEntity resolveOccupantUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return users.findByIdAndRole_Code(userId, TENANT_ROLE)
                .orElseThrow(
                        () -> new ContractBusinessException("Occupant user is missing or invalid"));
    }

    private LockedOccupantContext lockContract(Long contractId) {
        ContractEntity initial =
                contracts
                        .findById(contractId)
                        .orElseThrow(() -> new ContractNotFoundException(contractId));
        RoomEntity room =
                rooms.findByIdForUpdate(initial.getRoom().getId())
                        .orElseThrow(() -> new ContractBusinessException("Room not found"));
        ContractEntity contract =
                contracts
                        .findByIdForUpdate(contractId)
                        .orElseThrow(() -> new ContractNotFoundException(contractId));
        return new LockedOccupantContext(room, contract);
    }

    private void ensureEditable(ContractEntity contract) {
        if (contract.getStatus() == ContractStatus.TERMINATED
                || contract.getStatus() == ContractStatus.EXPIRED
                || contract.getStatus() == ContractStatus.CANCELLED) {
            throw new ContractBusinessException(
                    "Occupants cannot be changed for a closed contract");
        }
    }

    /**
     * Counts everyone still staying on or after the move-in date (not only those present on that
     * exact day), so a back-dated move-in cannot bypass the room limit. The contract's own
     * occupants are always checked, so a DRAFT/PENDING contract cannot be overfilled either.
     */
    private void ensureCapacityForNewOccupant(
            ContractEntity contract, RoomEntity room, LocalDate moveInDate) {
        long inContract = occupants.countStayingByContractIdFrom(contract.getId(), moveInDate);
        if (inContract + 1 > room.getMaxOccupants()) {
            throw new ContractBusinessException("Phòng đã đủ số người tối đa, không thể thêm người");
        }
        if (!room.isAllowsSharing() && inContract > 0) {
            throw new ContractBusinessException("Phòng không cho phép ở ghép");
        }
        if (EFFECTIVE_STATUSES.contains(contract.getStatus())) {
            long inRoom =
                    occupants.countStayingByRoomIdFrom(room.getId(), EFFECTIVE_STATUSES, moveInDate);
            if (inRoom + 1 > room.getMaxOccupants()) {
                throw new ContractBusinessException("Phòng đã đủ số người tối đa, không thể thêm người");
            }
            if (!room.isAllowsSharing() && inRoom > 0) {
                throw new ContractBusinessException("Phòng không cho phép ở ghép");
            }
        }
    }

    /** Runs after the occupant change is applied, so the counts already include it. */
    private void ensureCapacityAfterChange(
            ContractEntity contract, RoomEntity room, LocalDate fromDate) {
        long inContract = occupants.countStayingByContractIdFrom(contract.getId(), fromDate);
        long inRoom =
                EFFECTIVE_STATUSES.contains(contract.getStatus())
                        ? occupants.countStayingByRoomIdFrom(
                                room.getId(), EFFECTIVE_STATUSES, fromDate)
                        : 0;
        if (Math.max(inContract, inRoom) > room.getMaxOccupants()) {
            throw new ContractBusinessException("Phòng đã đủ số người tối đa, không thể thêm người");
        }
    }

    /**
     * Rejects adding the same person twice to one contract: same tenant account, same identity
     * document number, or same full name together with the same phone number.
     */
    private void ensureNotDuplicateInContract(
            Long contractId,
            Long excludedOccupantId,
            AppUserEntity user,
            String fullName,
            String phoneNumber,
            String identityNumber) {
        LocalDate today = LocalDate.now();
        boolean duplicate =
                occupants.findByContract_IdOrderByMoveInDateAsc(contractId).stream()
                        .filter(other -> !other.getId().equals(excludedOccupantId))
                        .filter(other -> isStaying(other.getMoveOutDate(), today))
                        .anyMatch(
                                other ->
                                        (user != null
                                                        && other.getUser() != null
                                                        && user.getId()
                                                                .equals(other.getUser().getId()))
                                                || sameNonBlank(
                                                        identityNumber,
                                                        other.getIdentityDocumentNumber())
                                                || (fullName.equalsIgnoreCase(other.getFullName())
                                                        && sameNonBlank(
                                                                phoneNumber,
                                                                other.getPhoneNumber())));
        if (duplicate) {
            throw new ContractBusinessException("Người này đã có trong danh sách người ở của phòng");
        }
    }

    private boolean isStaying(LocalDate moveOutDate, LocalDate asOf) {
        return moveOutDate == null || !moveOutDate.isBefore(asOf);
    }

    private boolean sameNonBlank(String value, String other) {
        return value != null
                && !value.isBlank()
                && other != null
                && value.equalsIgnoreCase(other.trim());
    }

    private void synchronizeRoom(RoomEntity room) {
        if (room.getStatus().equals(RoomStatus.MAINTENANCE.name())
                || room.getStatus().equals(RoomStatus.INACTIVE.name())) {
            return;
        }
        long count =
                occupants.countCurrentByRoomId(room.getId(), EFFECTIVE_STATUSES, LocalDate.now());
        if (count == 0) {
            room.changeStatus(RoomStatus.VACANT);
        } else if (count >= room.getMaxOccupants()) {
            room.changeStatus(RoomStatus.FULL);
        } else {
            room.changeStatus(RoomStatus.OCCUPIED);
        }
    }

    private void validateDate(LocalDate moveInDate, LocalDate moveOutDate) {
        if (moveOutDate != null && moveOutDate.isBefore(moveInDate)) {
            throw new ContractBusinessException("Move-out date cannot be before move-in date");
        }
    }

    private void writeHistory(
            ContractEntity contract,
            String action,
            String before,
            String after,
            AppUserEntity admin) {
        histories.save(ContractHistoryEntity.create(contract, action, before, after, admin));
    }

    private String snapshot(ContractOccupantEntity occupant) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("occupantId", occupant.getId());
        data.put("contractId", occupant.getContract().getId());
        data.put("userId", occupant.getUser() == null ? null : occupant.getUser().getId());
        data.put("fullName", occupant.getFullName());
        data.put("phoneNumber", occupant.getPhoneNumber());
        data.put("isPrimaryTenant", occupant.isPrimaryTenant());
        data.put("moveInDate", occupant.getMoveInDate());
        data.put("moveOutDate", occupant.getMoveOutDate());
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException ex) {
            throw new ContractBusinessException("Unable to serialize occupant history");
        }
    }

    private ContractOccupantResponse toResponse(ContractOccupantEntity occupant) {
        return new ContractOccupantResponse(
                occupant.getId(),
                occupant.getContract().getId(),
                occupant.getUser() == null ? null : occupant.getUser().getId(),
                occupant.getFullName(),
                occupant.getPhoneNumber(),
                occupant.getIdentityDocumentNumber(),
                occupant.isPrimaryTenant(),
                occupant.getMoveInDate(),
                occupant.getMoveOutDate(),
                occupant.getCreatedAt());
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private record LockedOccupantContext(RoomEntity room, ContractEntity contract) {}
}
