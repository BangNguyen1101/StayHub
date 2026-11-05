package com.quanlynhatro.contract.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.billing.repository.InvoiceRepository;
import com.quanlynhatro.contract.api.ContractCancelRequest;
import com.quanlynhatro.contract.api.ContractCreateRequest;
import com.quanlynhatro.contract.api.ContractHistoryResponse;
import com.quanlynhatro.contract.api.ContractRenewRequest;
import com.quanlynhatro.contract.api.ContractResponse;
import com.quanlynhatro.contract.api.ContractStatus;
import com.quanlynhatro.contract.api.ContractSummaryResponse;
import com.quanlynhatro.contract.api.ContractTerminateRequest;
import com.quanlynhatro.contract.api.ContractUpdateRequest;
import com.quanlynhatro.contract.domain.ContractEntity;
import com.quanlynhatro.contract.domain.ContractHistoryEntity;
import com.quanlynhatro.contract.domain.ContractOccupantEntity;
import com.quanlynhatro.contract.repository.ContractHistoryRepository;
import com.quanlynhatro.contract.repository.ContractOccupantRepository;
import com.quanlynhatro.contract.repository.ContractRepository;
import com.quanlynhatro.maintenance.repository.MaintenanceRequestRepository;
import com.quanlynhatro.rental.api.RentalRequestStatus;
import com.quanlynhatro.rental.domain.RentalRequestEntity;
import com.quanlynhatro.rental.repository.RentalRequestRepository;
import com.quanlynhatro.room.api.RoomStatus;
import com.quanlynhatro.room.domain.RoomEntity;
import com.quanlynhatro.room.repository.RoomRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ContractManagementServiceImpl implements ContractManagementService {
    private static final Logger log = LoggerFactory.getLogger(ContractManagementServiceImpl.class);
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String TENANT_ROLE = "TENANT";
    private static final int EXPIRY_WARNING_DAYS = 30;
    private static final List<ContractStatus> EFFECTIVE_STATUSES =
            List.of(ContractStatus.ACTIVE, ContractStatus.EXPIRING);

    private final ContractRepository contracts;
    private final ContractOccupantRepository occupants;
    private final ContractHistoryRepository histories;
    private final AppUserRepository users;
    private final RoomRepository rooms;
    private final RentalRequestRepository rentalRequests;
    private final InvoiceRepository invoices;
    private final MaintenanceRequestRepository maintenanceRequests;
    private final ContractDocumentService documents;
    private final ObjectMapper objectMapper;

    public ContractManagementServiceImpl(
            ContractRepository contracts,
            ContractOccupantRepository occupants,
            ContractHistoryRepository histories,
            AppUserRepository users,
            RoomRepository rooms,
            RentalRequestRepository rentalRequests,
            ObjectMapper objectMapper) {
        this(
                contracts,
                occupants,
                histories,
                users,
                rooms,
                rentalRequests,
                null,
                null,
                null,
                objectMapper);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ContractManagementServiceImpl(
            ContractRepository contracts,
            ContractOccupantRepository occupants,
            ContractHistoryRepository histories,
            AppUserRepository users,
            RoomRepository rooms,
            RentalRequestRepository rentalRequests,
            InvoiceRepository invoices,
            MaintenanceRequestRepository maintenanceRequests,
            ContractDocumentService documents,
            ObjectMapper objectMapper) {
        this.contracts = contracts;
        this.occupants = occupants;
        this.histories = histories;
        this.users = users;
        this.rooms = rooms;
        this.rentalRequests = rentalRequests;
        this.invoices = invoices;
        this.maintenanceRequests = maintenanceRequests;
        this.documents = documents;
        this.objectMapper = objectMapper;
    }

    @Override
    public Page<ContractSummaryResponse> search(
            String keyword,
            ContractStatus status,
            Long tenantUserId,
            Long roomId,
            Pageable pageable) {
        return contracts
                .search(normalize(keyword), status, tenantUserId, roomId, pageable)
                .map(this::toSummary);
    }

    @Override
    public ContractResponse getById(Long contractId) {
        return toResponse(findContract(contractId));
    }

    @Override
    public ContractResponse getForTenant(String authenticatedUsername, Long contractId) {
        AppUserEntity tenant = findTenantByUsername(authenticatedUsername);
        ContractEntity contract = findContract(contractId);
        boolean primary = contract.getTenantUser().getId().equals(tenant.getId());
        boolean occupant = occupants.existsByContract_IdAndUser_Id(contractId, tenant.getId());
        if (!primary && !occupant) {
            throw new ContractAccessDeniedException();
        }
        return toResponse(contract);
    }

    @Override
    public List<ContractHistoryResponse> history(Long contractId) {
        findContract(contractId);
        return histories.findByContract_IdOrderByPerformedAtDesc(contractId).stream()
                .map(
                        history ->
                                new ContractHistoryResponse(
                                        history.getId(),
                                        history.getContract().getId(),
                                        history.getActionCode(),
                                        history.getBeforeData(),
                                        history.getAfterData(),
                                        history.getPerformedByUser().getId(),
                                        history.getPerformedAt()))
                .toList();
    }

    @Override
    @Transactional
    public ContractResponse create(ContractCreateRequest request, Long adminUserId) {
        AppUserEntity admin = requireAdmin(adminUserId);
        AppUserEntity tenant =
                users.findByIdAndRole_Code(request.tenantUserId(), TENANT_ROLE)
                        .orElseThrow(
                                () ->
                                        new ContractBusinessException(
                                                "Tenant is missing or invalid"));
        validateDateRange(request.startDate(), request.endDate());

        RoomEntity room = lockRoom(request.roomId());
        ensureRoomCanReceiveContract(room);
        RentalRequestEntity rentalRequest =
                validateRentalRequest(request.rentalRequestId(), tenant, room);

        if (contracts.existsByContractCodeIgnoreCase(request.contractCode().trim())) {
            throw new ContractBusinessException("Contract code already exists");
        }

        ContractEntity contract =
                ContractEntity.create(
                        request.contractCode().trim(),
                        tenant,
                        room,
                        rentalRequest,
                        request.startDate(),
                        request.endDate(),
                        request.monthlyRent(),
                        request.depositAmount(),
                        request.billingCycleMonths(),
                        request.dueDay(),
                        request.terms(),
                        request.notes(),
                        admin);
        contracts.save(contract);
        ContractOccupantEntity primary =
                ContractOccupantEntity.create(
                        contract,
                        tenant,
                        tenant.getFullName(),
                        tenant.getPhoneNumber(),
                        null,
                        true,
                        request.startDate());
        occupants.save(primary);
        writeHistory(contract, "CREATED", null, snapshot(contract), admin);
        return toResponse(contract);
    }

    @Override
    @Transactional
    public ContractResponse updateDraft(
            Long contractId, ContractUpdateRequest request, Long adminUserId) {
        AppUserEntity admin = requireAdmin(adminUserId);
        LockedContract locked = lockContractWithRoom(contractId);
        ContractEntity contract = locked.contract();
        ensureStatus(contract, ContractStatus.DRAFT);
        validateDateRange(request.startDate(), request.endDate());
        String before = snapshot(contract);
        contract.updateDetails(
                request.startDate(),
                request.endDate(),
                request.monthlyRent(),
                request.depositAmount(),
                request.billingCycleMonths(),
                request.dueDay(),
                request.terms(),
                request.notes());
        writeHistory(contract, "UPDATED", before, snapshot(contract), admin);
        return toResponse(contract);
    }

    @Override
    @Transactional
    public ContractResponse submitPending(Long contractId, Long adminUserId) {
        AppUserEntity admin = requireAdmin(adminUserId);
        LockedContract locked = lockContractWithRoom(contractId);
        ContractEntity contract = locked.contract();
        ensureStatus(contract, ContractStatus.DRAFT);
        String before = snapshot(contract);
        contract.changeStatus(ContractStatus.PENDING);
        writeHistory(contract, "SUBMITTED", before, snapshot(contract), admin);
        if (documents != null) {
            // The submitted terms are frozen into a document the tenant signs online.
            documents.issueContractDocument(contract, admin);
        }
        return toResponse(contract);
    }

    @Override
    @Transactional
    public ContractResponse activate(Long contractId, Long adminUserId) {
        AppUserEntity admin = requireAdmin(adminUserId);
        LockedContract locked = lockContractWithRoom(contractId);
        ContractEntity contract = locked.contract();
        RoomEntity room = locked.room();
        ensureStatus(contract, ContractStatus.PENDING);
        if (documents != null && !documents.isContractSigned(contract.getId())) {
            throw new ContractBusinessException(
                    "Người thuê chưa ký hợp đồng điện tử, chưa thể kích hoạt");
        }
        lockTenantForActivation(contract);
        ensurePrimaryOccupant(contract);
        ensureCapacity(room, contract);
        String before = snapshot(contract);
        contract.markActivated(LocalDateTime.now());
        synchronizeRoom(room);
        writeHistory(contract, "ACTIVATED", before, snapshot(contract), admin);
        return toResponse(contract);
    }

    @Override
    @Transactional
    public ContractResponse renew(Long contractId, ContractRenewRequest request, Long adminUserId) {
        AppUserEntity admin = requireAdmin(adminUserId);
        LockedContract locked = lockContractWithRoom(contractId);
        ContractEntity contract = locked.contract();
        if (contract.getStatus() != ContractStatus.ACTIVE
                && contract.getStatus() != ContractStatus.EXPIRING) {
            throw new ContractBusinessException("Only ACTIVE or EXPIRING contracts can be renewed");
        }
        if (!request.endDate().isAfter(contract.getEndDate())) {
            throw new ContractBusinessException(
                    "Renewal end date must be after the current end date");
        }
        String before = snapshot(contract);
        contract.renewUntil(request.endDate());
        writeHistory(contract, "RENEWED", before, snapshot(contract), admin);
        return toResponse(contract);
    }

    @Override
    @Transactional
    public ContractResponse terminate(
            Long contractId, ContractTerminateRequest request, Long adminUserId) {
        AppUserEntity admin = requireAdmin(adminUserId);
        LockedContract locked = lockContractWithRoom(contractId);
        ContractEntity contract = locked.contract();
        if (contract.getStatus() != ContractStatus.ACTIVE
                && contract.getStatus() != ContractStatus.EXPIRING) {
            throw new ContractBusinessException(
                    "Only ACTIVE or EXPIRING contracts can be terminated");
        }
        if (request.actualEndDate().isBefore(contract.getStartDate())) {
            throw new ContractBusinessException("Actual end date cannot be before start date");
        }
        String before = snapshot(contract);
        contract.markTerminated(request.actualEndDate(), LocalDateTime.now());
        if (documents != null) {
            documents.voidAwaiting(contractId);
        }
        synchronizeRoom(locked.room());
        writeHistory(contract, "TERMINATED", before, snapshot(contract), admin);
        return toResponse(contract);
    }

    @Override
    @Transactional
    public ContractResponse cancel(
            Long contractId, ContractCancelRequest request, Long adminUserId) {
        AppUserEntity admin = requireAdmin(adminUserId);
        LockedContract locked = lockContractWithRoom(contractId);
        ContractEntity contract = locked.contract();
        if (contract.getStatus() != ContractStatus.DRAFT
                && contract.getStatus() != ContractStatus.PENDING) {
            throw new ContractBusinessException("Only DRAFT or PENDING contracts can be cancelled");
        }
        String before = snapshot(contract);
        contract.changeStatus(com.quanlynhatro.contract.api.ContractStatus.CANCELLED);
        if (documents != null) {
            documents.voidAwaiting(contractId);
        }
        writeHistory(contract, "CANCELLED", before, snapshot(contract), admin);
        return toResponse(contract);
    }

    @Override
    @Transactional
    public void deleteOldContract(Long contractId, Long adminUserId) {
        requireAdmin(adminUserId);
        LockedContract locked = lockContractWithRoom(contractId);
        ContractEntity contract = locked.contract();
        if (contract.getStatus() != ContractStatus.TERMINATED
                && contract.getStatus() != ContractStatus.EXPIRED
                && contract.getStatus() != ContractStatus.CANCELLED) {
            throw new ContractBusinessException(
                    "Only terminated, expired, or cancelled contracts can be deleted");
        }
        if (invoices != null && invoices.existsByContract_Id(contractId)) {
            throw new ContractBusinessException("Contract with invoices cannot be deleted");
        }
        if (documents != null) {
            documents.deleteForContract(contractId);
        }
        histories.deleteByContract_Id(contractId);
        occupants.deleteByContract_Id(contractId);
        if (maintenanceRequests != null) {
            maintenanceRequests.clearContractReferences(contractId);
        }
        contracts.delete(contract);
        synchronizeRoom(locked.room());
    }

    @Override
    @Transactional
    public void processScheduledExpiry(LocalDate today) {
        AppUserEntity schedulerActor =
                users.findFirstByRole_CodeAndActiveTrueOrderByIdAsc(ADMIN_ROLE)
                        .orElseThrow(
                                () ->
                                        new ContractBusinessException(
                                                "No active ADMIN available for scheduled contract"
                                                    + " processing"));
        LocalDate horizon = today.plusDays(EXPIRY_WARNING_DAYS);
        List<ContractEntity> candidates =
                contracts.findByStatusInAndEndDateLessThanEqualOrderByIdAsc(
                        EFFECTIVE_STATUSES, horizon);
        for (ContractEntity candidate : candidates) {
            try {
                processScheduledContract(candidate.getId(), today, schedulerActor);
            } catch (RuntimeException ex) {
                log.warn(
                        "Scheduled contract expiry processing failed for contract {}",
                        candidate.getId(),
                        ex);
            }
        }
    }

    private void processScheduledContract(
            Long contractId, LocalDate today, AppUserEntity schedulerActor) {
        LockedContract locked = lockContractWithRoom(contractId);
        ContractEntity contract = locked.contract();
        LocalDate endDate = contract.getEndDate();
        if (!EFFECTIVE_STATUSES.contains(contract.getStatus())) {
            return;
        }
        if (endDate.isBefore(today)) {
            String before = snapshot(contract);
            contract.markExpired(endDate);
            synchronizeRoom(locked.room(), today);
            writeHistory(contract, "EXPIRED", before, snapshot(contract), schedulerActor);
        } else if (contract.getStatus() == ContractStatus.ACTIVE
                && !endDate.isAfter(today.plusDays(EXPIRY_WARNING_DAYS))) {
            String before = snapshot(contract);
            contract.changeStatus(ContractStatus.EXPIRING);
            writeHistory(contract, "MARKED_EXPIRING", before, snapshot(contract), schedulerActor);
        }
    }

    private AppUserEntity requireAdmin(Long userId) {
        return users.findByIdAndRole_Code(userId, ADMIN_ROLE)
                .orElseThrow(() -> new ContractAccessDeniedException());
    }

    private AppUserEntity findTenantByUsername(String username) {
        return users.findByUsernameIgnoreCaseOrEmailIgnoreCase(username, username)
                .filter(
                        user ->
                                user.getRole() != null
                                        && TENANT_ROLE.equals(user.getRole().getCode()))
                .orElseThrow(() -> new ContractAccessDeniedException());
    }

    private ContractEntity findContract(Long contractId) {
        return contracts
                .findById(contractId)
                .orElseThrow(() -> new ContractNotFoundException(contractId));
    }

    private RoomEntity lockRoom(Long roomId) {
        return rooms.findByIdForUpdate(roomId)
                .orElseThrow(() -> new ContractBusinessException("Room not found: " + roomId));
    }

    private LockedContract lockContractWithRoom(Long contractId) {
        ContractEntity initial = findContract(contractId);
        RoomEntity room = lockRoom(initial.getRoom().getId());
        ContractEntity locked =
                contracts
                        .findByIdForUpdate(contractId)
                        .orElseThrow(() -> new ContractNotFoundException(contractId));
        return new LockedContract(room, locked);
    }

    private RentalRequestEntity validateRentalRequest(
            Long requestId, AppUserEntity tenant, RoomEntity room) {
        if (requestId == null) {
            return null;
        }
        RentalRequestEntity request =
                rentalRequests
                        .findById(requestId)
                        .orElseThrow(
                                () -> new ContractBusinessException("Rental request not found"));
        if (request.getStatus() != RentalRequestStatus.APPROVED
                || !request.getApplicantUser().getId().equals(tenant.getId())
                || !request.getRoom().getId().equals(room.getId())) {
            throw new ContractBusinessException(
                    "Rental request is not approved for this tenant and room");
        }
        return request;
    }

    private void ensureRoomCanReceiveContract(RoomEntity room) {
        if (room.getStatus().equals(RoomStatus.MAINTENANCE.name())
                || room.getStatus().equals(RoomStatus.INACTIVE.name())) {
            throw new ContractBusinessException("Room is not available for a contract");
        }
    }

    private void ensureCapacity(RoomEntity room, ContractEntity contract) {
        // Count everyone still staying from today on, including future move-ins.
        LocalDate today = LocalDate.now();
        long existing = occupants.countStayingByRoomIdFrom(room.getId(), EFFECTIVE_STATUSES, today);
        long incoming = occupants.countStayingByContractIdFrom(contract.getId(), today);
        if (existing + incoming > room.getMaxOccupants()) {
            throw new ContractBusinessException("Phòng đã đủ số người tối đa, không thể thêm người");
        }
        if (!room.isAllowsSharing() && existing > 0) {
            throw new ContractBusinessException("Phòng không cho phép ở ghép");
        }
    }

    private void synchronizeRoom(RoomEntity room) {
        synchronizeRoom(room, LocalDate.now());
    }

    /** Recomputes room status from occupants effective on {@code asOf} (the scheduler run date). */
    private void synchronizeRoom(RoomEntity room, LocalDate asOf) {
        if (room.getStatus().equals(RoomStatus.MAINTENANCE.name())
                || room.getStatus().equals(RoomStatus.INACTIVE.name())) {
            return;
        }
        long count = occupants.countCurrentByRoomId(room.getId(), EFFECTIVE_STATUSES, asOf);
        if (count == 0) {
            room.changeStatus(RoomStatus.VACANT);
        } else if (count >= room.getMaxOccupants()) {
            room.changeStatus(RoomStatus.FULL);
        } else {
            room.changeStatus(RoomStatus.OCCUPIED);
        }
    }

    private void ensurePrimaryOccupant(ContractEntity contract) {
        ContractOccupantEntity primary =
                occupants
                        .findByContract_IdAndPrimaryTenantTrue(contract.getId())
                        .orElseThrow(
                                () ->
                                        new ContractBusinessException(
                                                "Contract must have one primary tenant"));
        if (!primary.getUser().getId().equals(contract.getTenantUser().getId())) {
            throw new ContractBusinessException("Primary occupant must match TenantUserId");
        }
        if (occupants.countByContract_IdAndPrimaryTenantTrue(contract.getId()) != 1) {
            throw new ContractBusinessException("Contract must have exactly one primary tenant");
        }
    }

    private void lockTenantForActivation(ContractEntity contract) {
        Long tenantId = contract.getTenantUser().getId();
        users.findByIdForUpdate(tenantId)
                .orElseThrow(() -> new ContractBusinessException("Tenant is missing or invalid"));
        if (contracts.existsByTenantUser_IdAndStatusIn(tenantId, EFFECTIVE_STATUSES)) {
            throw new ContractBusinessException("Tenant already has an effective contract");
        }
    }

    private void ensureStatus(ContractEntity contract, ContractStatus expected) {
        if (contract.getStatus() != expected) {
            throw new ContractBusinessException("Contract must be " + expected);
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new ContractBusinessException("End date cannot be before start date");
        }
    }

    private void writeHistory(
            ContractEntity contract,
            String action,
            String before,
            String after,
            AppUserEntity actor) {
        histories.save(ContractHistoryEntity.create(contract, action, before, after, actor));
    }

    private String snapshot(ContractEntity contract) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("contractId", contract.getId());
        data.put("contractCode", contract.getContractCode());
        data.put("tenantUserId", contract.getTenantUser().getId());
        data.put("roomId", contract.getRoom().getId());
        data.put("startDate", contract.getStartDate());
        data.put("endDate", contract.getEndDate());
        data.put("actualEndDate", contract.getActualEndDate());
        data.put("monthlyRent", contract.getMonthlyRent());
        data.put("depositAmount", contract.getDepositAmount());
        data.put("billingCycleMonths", contract.getBillingCycleMonths());
        data.put("dueDay", contract.getDueDay());
        data.put("status", contract.getStatus());
        data.put("notes", contract.getNotes());
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException ex) {
            throw new ContractBusinessException("Unable to serialize contract history");
        }
    }

    private ContractSummaryResponse toSummary(ContractEntity contract) {
        return new ContractSummaryResponse(
                contract.getId(),
                contract.getContractCode(),
                contract.getTenantUser().getId(),
                contract.getTenantUser().getFullName(),
                contract.getRoom().getId(),
                contract.getRoom().getRoomCode(),
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getMonthlyRent(),
                contract.getStatus());
    }

    private ContractResponse toResponse(ContractEntity contract) {
        return new ContractResponse(
                contract.getId(),
                contract.getContractCode(),
                contract.getTenantUser().getId(),
                contract.getRoom().getId(),
                contract.getRoom().getRoomCode(),
                contract.getRentalRequest() == null ? null : contract.getRentalRequest().getId(),
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getActualEndDate(),
                contract.getMonthlyRent(),
                contract.getDepositAmount(),
                contract.getBillingCycleMonths(),
                contract.getDueDay(),
                contract.getStatus(),
                contract.getTerms(),
                contract.getNotes(),
                contract.getActivatedAt(),
                contract.getTerminatedAt(),
                contract.getCreatedAt(),
                contract.getUpdatedAt());
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private record LockedContract(RoomEntity room, ContractEntity contract) {}
}
