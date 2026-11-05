package com.quanlynhatro.rental.service;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.rental.api.*;
import com.quanlynhatro.rental.domain.RentalRequestEntity;
import com.quanlynhatro.rental.domain.RentalRequestHistoryEntity;
import com.quanlynhatro.rental.repository.RentalRequestHistoryRepository;
import com.quanlynhatro.rental.repository.RentalRequestRepository;
import com.quanlynhatro.room.api.RoomStatus;
import com.quanlynhatro.room.domain.RoomEntity;
import com.quanlynhatro.room.repository.RoomRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RentalRequestServiceImpl implements RentalRequestService {
    private static final DateTimeFormatter REQUEST_CODE_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final EnumSet<RentalRequestStatus> OPEN_STATUSES =
            EnumSet.of(RentalRequestStatus.PENDING, RentalRequestStatus.NEEDS_INFO);

    private final RentalRequestRepository rentalRequestRepository;
    private final RentalRequestHistoryRepository historyRepository;
    private final RoomRepository roomRepository;
    private final AppUserRepository appUserRepository;

    public RentalRequestServiceImpl(
            RentalRequestRepository rentalRequestRepository,
            RentalRequestHistoryRepository historyRepository,
            RoomRepository roomRepository,
            AppUserRepository appUserRepository) {
        this.rentalRequestRepository = rentalRequestRepository;
        this.historyRepository = historyRepository;
        this.roomRepository = roomRepository;
        this.appUserRepository = appUserRepository;
    }

    @Override
    @Transactional
    public RentalRequestResponse submit(
            Authentication authentication, Long roomId, RentalRequestCreateRequest request) {
        AppUserEntity tenant = requireRole(authentication, "TENANT");
        RoomEntity room =
                roomRepository
                        .findByIdForUpdate(roomId)
                        .orElseThrow(
                                () ->
                                        new RentalRequestBusinessException(
                                                "Room not found: " + roomId));
        validateVacantAndCapacity(room, request.expectedOccupantCount());
        if (rentalRequestRepository.existsByApplicantUser_IdAndRoom_IdAndStatusIn(
                tenant.getId(), roomId, OPEN_STATUSES)) {
            throw new RentalRequestBusinessException(
                    "An open rental request already exists for this room");
        }
        RentalRequestEntity entity =
                RentalRequestEntity.create(
                        generateUniqueRequestCode(),
                        tenant,
                        room,
                        request.contactName(),
                        request.contactPhone(),
                        request.contactEmail(),
                        request.expectedOccupantCount(),
                        request.expectedMoveInDate(),
                        request.note());
        RentalRequestEntity saved = rentalRequestRepository.save(entity);
        historyRepository.save(
                RentalRequestHistoryEntity.create(
                        saved, null, RentalRequestStatus.PENDING, null, tenant));
        return toResponse(saved);
    }

    @Override
    public Page<RentalRequestSummaryResponse> getMyRequests(
            Authentication authentication, RentalRequestStatus status, Pageable pageable) {
        AppUserEntity tenant = requireRole(authentication, "TENANT");
        return rentalRequestRepository
                .search(null, status, tenant.getId(), null, pageable)
                .map(this::toSummary);
    }

    @Override
    public RentalRequestResponse getMyRequest(Authentication authentication, Long id) {
        AppUserEntity tenant = requireRole(authentication, "TENANT");
        RentalRequestEntity entity = findRequest(id);
        requireOwner(entity, tenant);
        return toResponse(entity);
    }

    @Override
    @Transactional
    public RentalRequestResponse updateInformation(
            Authentication authentication, Long id, RentalRequestInfoUpdateRequest request) {
        AppUserEntity tenant = requireRole(authentication, "TENANT");
        RentalRequestEntity entity =
                rentalRequestRepository
                        .findByIdForUpdate(id)
                        .orElseThrow(() -> new RentalRequestNotFoundException(id));
        requireOwner(entity, tenant);
        if (entity.getStatus() != RentalRequestStatus.NEEDS_INFO) {
            throw new RentalRequestBusinessException("Only NEEDS_INFO requests can be updated");
        }
        RoomEntity room =
                roomRepository
                        .findByIdForUpdate(entity.getRoom().getId())
                        .orElseThrow(
                                () ->
                                        new RentalRequestBusinessException(
                                                "Room not found: " + entity.getRoom().getId()));
        validateVacantAndCapacity(room, request.expectedOccupantCount());
        entity.updateInformation(
                request.contactName(),
                request.contactPhone(),
                request.contactEmail(),
                request.expectedOccupantCount(),
                request.expectedMoveInDate(),
                request.note());
        return toResponse(entity);
    }

    @Override
    @Transactional
    public RentalRequestResponse cancel(Authentication authentication, Long id) {
        AppUserEntity tenant = requireRole(authentication, "TENANT");
        RentalRequestEntity entity =
                rentalRequestRepository
                        .findByIdForUpdate(id)
                        .orElseThrow(() -> new RentalRequestNotFoundException(id));
        requireOwner(entity, tenant);
        if (!OPEN_STATUSES.contains(entity.getStatus())) {
            throw new RentalRequestBusinessException(
                    "Only PENDING or NEEDS_INFO requests can be cancelled");
        }
        changeStatus(entity, RentalRequestStatus.CANCELLED, tenant, "Cancelled by applicant");
        return toResponse(entity);
    }

    @Override
    public Page<RentalRequestSummaryResponse> searchForAdmin(
            Authentication authentication,
            String keyword,
            RentalRequestStatus status,
            Long applicantUserId,
            Long roomId,
            Pageable pageable) {
        requireRole(authentication, "ADMIN");
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return rentalRequestRepository
                .search(normalizedKeyword, status, applicantUserId, roomId, pageable)
                .map(this::toSummary);
    }

    @Override
    public RentalRequestResponse getForAdmin(Authentication authentication, Long id) {
        requireRole(authentication, "ADMIN");
        return toResponse(findRequest(id));
    }

    @Override
    @Transactional
    public RentalRequestResponse decide(
            Authentication authentication, Long id, RentalRequestDecisionRequest decision) {
        AppUserEntity admin = requireRole(authentication, "ADMIN");
        RentalRequestEntity entity =
                rentalRequestRepository
                        .findByIdForUpdate(id)
                        .orElseThrow(() -> new RentalRequestNotFoundException(id));
        if (!OPEN_STATUSES.contains(entity.getStatus())) {
            throw new RentalRequestBusinessException(
                    "Only PENDING or NEEDS_INFO requests can be processed");
        }
        RentalRequestStatus target = decision.status();
        if (target != RentalRequestStatus.APPROVED
                && target != RentalRequestStatus.REJECTED
                && target != RentalRequestStatus.NEEDS_INFO) {
            throw new RentalRequestBusinessException("Unsupported decision status: " + target);
        }
        if ((target == RentalRequestStatus.REJECTED || target == RentalRequestStatus.NEEDS_INFO)
                && !StringUtils.hasText(decision.note())) {
            throw new RentalRequestBusinessException(
                    "A processing note is required for this decision");
        }
        if (target == RentalRequestStatus.APPROVED) {
            RoomEntity room =
                    roomRepository
                            .findByIdForUpdate(entity.getRoom().getId())
                            .orElseThrow(
                                    () ->
                                            new RentalRequestBusinessException(
                                                    "Room not found: " + entity.getRoom().getId()));
            validateVacantAndCapacity(room, entity.getExpectedOccupantCount());
        }
        changeStatus(entity, target, admin, decision.note());
        return toResponse(entity);
    }

    @Override
    public List<RentalRequestHistoryResponse> getHistoryForAdmin(
            Authentication authentication, Long id) {
        requireRole(authentication, "ADMIN");
        findRequest(id);
        return historyRepository.findByRentalRequest_IdOrderByChangedAtDesc(id).stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    private void changeStatus(
            RentalRequestEntity entity,
            RentalRequestStatus target,
            AppUserEntity actor,
            String note) {
        RentalRequestStatus oldStatus = entity.getStatus();
        entity.changeStatus(target, actor, LocalDateTime.now(ZoneOffset.UTC), note);
        historyRepository.save(
                RentalRequestHistoryEntity.create(entity, oldStatus, target, note, actor));
    }

    private void validateVacantAndCapacity(RoomEntity room, Integer count) {
        if (!RoomStatus.VACANT.name().equals(room.getStatus())) {
            throw new RentalRequestBusinessException(
                    "Only VACANT rooms can receive or approve requests");
        }
        if (count == null
                || count <= 0
                || room.getMaxOccupants() == null
                || count > room.getMaxOccupants()) {
            throw new RentalRequestBusinessException(
                    "Expected occupant count exceeds room capacity");
        }
    }

    private String generateUniqueRequestCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String suffix =
                    UUID.randomUUID()
                            .toString()
                            .replace("-", "")
                            .substring(0, 8)
                            .toUpperCase(Locale.ROOT);
            String code =
                    "RR-"
                            + LocalDateTime.now(ZoneOffset.UTC).format(REQUEST_CODE_DATE)
                            + "-"
                            + suffix;
            if (!rentalRequestRepository.existsByRequestCode(code)) return code;
        }
        throw new RentalRequestBusinessException("Could not generate a unique request code");
    }

    private AppUserEntity requireRole(Authentication authentication, String roleCode) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !StringUtils.hasText(authentication.getName())) {
            throw new RentalRequestAccessDeniedException();
        }
        AppUserEntity user =
                appUserRepository
                        .findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                authentication.getName(), authentication.getName())
                        .orElseThrow(RentalRequestAccessDeniedException::new);
        if (!user.isActive()
                || user.getRole() == null
                || !roleCode.equalsIgnoreCase(user.getRole().getCode())) {
            throw new RentalRequestAccessDeniedException();
        }
        return user;
    }

    private RentalRequestEntity findRequest(Long id) {
        return rentalRequestRepository
                .findById(id)
                .orElseThrow(() -> new RentalRequestNotFoundException(id));
    }

    private void requireOwner(RentalRequestEntity entity, AppUserEntity tenant) {
        if (entity.getApplicantUser() == null
                || !tenant.getId().equals(entity.getApplicantUser().getId())) {
            throw new RentalRequestAccessDeniedException();
        }
    }

    private RentalRequestResponse toResponse(RentalRequestEntity entity) {
        return new RentalRequestResponse(
                entity.getId(),
                entity.getRequestCode(),
                entity.getApplicantUser().getId(),
                entity.getRoom().getId(),
                entity.getRoom().getRoomCode(),
                entity.getContactName(),
                entity.getContactPhone(),
                entity.getContactEmail(),
                entity.getExpectedOccupantCount(),
                entity.getExpectedMoveInDate(),
                entity.getNote(),
                entity.getStatus(),
                entity.getProcessedByUser() == null ? null : entity.getProcessedByUser().getId(),
                entity.getProcessedAt(),
                entity.getProcessingNote(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private RentalRequestSummaryResponse toSummary(RentalRequestEntity entity) {
        return new RentalRequestSummaryResponse(
                entity.getId(),
                entity.getRequestCode(),
                entity.getRoom().getId(),
                entity.getRoom().getRoomCode(),
                entity.getContactName(),
                entity.getExpectedOccupantCount(),
                entity.getExpectedMoveInDate(),
                entity.getStatus(),
                entity.getCreatedAt());
    }

    private RentalRequestHistoryResponse toHistoryResponse(RentalRequestHistoryEntity history) {
        return new RentalRequestHistoryResponse(
                history.getId(),
                history.getRentalRequest().getId(),
                history.getOldStatus(),
                history.getNewStatus(),
                history.getNote(),
                history.getChangedByUser().getId(),
                history.getChangedAt());
    }
}
