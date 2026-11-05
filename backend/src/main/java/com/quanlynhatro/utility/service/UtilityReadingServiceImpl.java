package com.quanlynhatro.utility.service;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.room.domain.RoomEntity;
import com.quanlynhatro.room.repository.RoomRepository;
import com.quanlynhatro.utility.api.UtilityReadingCreateRequest;
import com.quanlynhatro.utility.api.UtilityReadingLockRequest;
import com.quanlynhatro.utility.api.UtilityReadingResponse;
import com.quanlynhatro.utility.api.UtilityReadingUpdateRequest;
import com.quanlynhatro.utility.api.UtilityType;
import com.quanlynhatro.utility.domain.UtilityReadingEntity;
import com.quanlynhatro.utility.repository.UtilityReadingRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional(readOnly = true)
public class UtilityReadingServiceImpl implements UtilityReadingService {
    private final UtilityReadingRepository readings;
    private final RoomRepository rooms;
    private final AppUserRepository users;

    public UtilityReadingServiceImpl(
            UtilityReadingRepository readings, RoomRepository rooms, AppUserRepository users) {
        this.readings = readings;
        this.rooms = rooms;
        this.users = users;
    }

    @Override
    public Page<UtilityReadingResponse> search(
            Long roomId, UtilityType utilityType, Boolean locked, Pageable pageable) {
        return readings.search(
                        roomId, utilityType == null ? null : utilityType.name(), locked, pageable)
                .map(this::toResponse);
    }

    @Override
    public UtilityReadingResponse getById(Long id) {
        return toResponse(
                readings.findById(id).orElseThrow(() -> new UtilityReadingNotFoundException(id)));
    }

    @Override
    @Transactional
    public UtilityReadingResponse create(
            UtilityReadingCreateRequest request, Authentication authentication) {
        AppUserEntity actor = actor(authentication);
        RoomEntity room =
                rooms.findByIdForUpdate(request.roomId())
                        .orElseThrow(() -> new RoomForUtilityNotFoundException(request.roomId()));
        String type = request.utilityType().name();
        if (readings.existsByRoom_IdAndUtilityTypeAndBillingPeriod(
                request.roomId(), type, request.billingPeriod())) {
            throw new DuplicateUtilityReadingException(
                    request.roomId(), type, request.billingPeriod());
        }
        validateReadings(
                request.readingStart(),
                request.readingEnd(),
                request.roomId(),
                type,
                request.billingPeriod());
        return toResponse(
                readings.save(
                        UtilityReadingEntity.create(
                                room,
                                type,
                                request.billingPeriod(),
                                request.readingStart(),
                                request.readingEnd(),
                                request.readingDate(),
                                request.note(),
                                actor)));
    }

    @Override
    @Transactional
    public UtilityReadingResponse update(
            Long id, UtilityReadingUpdateRequest request, Authentication authentication) {
        actor(authentication);
        UtilityReadingEntity reading =
                readings.findByIdForUpdate(id)
                        .orElseThrow(() -> new UtilityReadingNotFoundException(id));
        if (reading.isLocked()) throw new LockedUtilityReadingException(id);
        validateReadings(
                request.readingStart(),
                request.readingEnd(),
                reading.getRoom().getId(),
                reading.getUtilityType(),
                reading.getBillingPeriod());
        reading.updateDetails(
                request.readingStart(),
                request.readingEnd(),
                request.readingDate(),
                request.note());
        return toResponse(reading);
    }

    @Override
    @Transactional
    public UtilityReadingResponse lock(
            Long id, UtilityReadingLockRequest request, Authentication authentication) {
        actor(authentication);
        UtilityReadingEntity reading =
                readings.findByIdForUpdate(id)
                        .orElseThrow(() -> new UtilityReadingNotFoundException(id));
        if (reading.isLocked() && !request.locked()) throw new LockedUtilityReadingException(id);
        if (Boolean.TRUE.equals(request.locked())) reading.lock();
        return toResponse(reading);
    }

    private AppUserEntity actor(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            throw new UtilityBusinessException("Authenticated user is required");
        return users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                        authentication.getName(), authentication.getName())
                .orElseThrow(
                        () -> new UtilityBusinessException("Authenticated user was not found"));
    }

    private void validateReadings(
            BigDecimal start,
            BigDecimal end,
            Long roomId,
            String utilityType,
            String billingPeriod) {
        if (start == null || end == null || end.compareTo(start) < 0)
            throw new InvalidUtilityReadingException();
        readings.findPreviousOne(roomId, utilityType, billingPeriod)
                .ifPresent(
                        previous -> {
                            if (start.compareTo(previous.getReadingEnd()) != 0)
                                throw new UtilityReadingSequenceException(
                                        roomId,
                                        utilityType,
                                        billingPeriod,
                                        previous.getReadingEnd(),
                                        start);
                        });
        readings.findNextOne(roomId, utilityType, billingPeriod)
                .ifPresent(
                        next -> {
                            if (end.compareTo(next.getReadingStart()) != 0)
                                throw new UtilityReadingSequenceException(
                                        roomId,
                                        utilityType,
                                        billingPeriod,
                                        next.getReadingStart(),
                                        end);
                        });
    }

    private UtilityReadingResponse toResponse(UtilityReadingEntity entity) {
        return new UtilityReadingResponse(
                entity.getId(),
                entity.getRoom().getId(),
                UtilityType.valueOf(entity.getUtilityType()),
                entity.getBillingPeriod(),
                entity.getReadingStart(),
                entity.getReadingEnd(),
                entity.getConsumption(),
                entity.getReadingDate(),
                entity.isLocked(),
                entity.getNote(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
