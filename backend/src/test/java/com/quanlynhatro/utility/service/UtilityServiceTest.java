package com.quanlynhatro.utility.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.room.domain.RoomEntity;
import com.quanlynhatro.room.repository.RoomRepository;
import com.quanlynhatro.utility.api.FeeConfigCreateRequest;
import com.quanlynhatro.utility.api.FeeConfigUpdateRequest;
import com.quanlynhatro.utility.api.FeePricingMode;
import com.quanlynhatro.utility.api.ServiceCreateRequest;
import com.quanlynhatro.utility.api.UtilityReadingCreateRequest;
import com.quanlynhatro.utility.api.UtilityReadingUpdateRequest;
import com.quanlynhatro.utility.api.UtilityType;
import com.quanlynhatro.utility.domain.FeeConfigEntity;
import com.quanlynhatro.utility.domain.UtilityReadingEntity;
import com.quanlynhatro.utility.repository.FeeConfigRepository;
import com.quanlynhatro.utility.repository.ServiceRepository;
import com.quanlynhatro.utility.repository.UtilityReadingRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class UtilityServiceTest {
    @Mock FeeConfigRepository feeConfigs;
    @Mock ServiceRepository services;
    @Mock UtilityReadingRepository readings;
    @Mock RoomRepository rooms;
    @Mock AppUserRepository users;
    @Mock Authentication authentication;
    @Mock AppUserEntity actor;
    @Mock RoomEntity room;
    @Mock UtilityReadingEntity previous;
    @Mock UtilityReadingEntity next;
    @Mock UtilityReadingEntity lockedReading;
    @Mock FeeConfigEntity feeConfig;

    @BeforeEach
    void authenticatedActor() {
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getName()).thenReturn("admin");
        lenient()
                .when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("admin", "admin"))
                .thenReturn(Optional.of(actor));
    }

    @Test
    void feeConfigRejectsOverlappingPeriod() {
        FeeConfigService service = new FeeConfigServiceImpl(feeConfigs, users);
        FeeConfigCreateRequest request =
                new FeeConfigCreateRequest(
                        "ELEC",
                        "Electricity",
                        "kWh",
                        new BigDecimal("3500.00"),
                        FeePricingMode.PER_USAGE,
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31),
                        true);
        when(feeConfigs.existsOverlappingPeriod(
                        "ELEC", request.effectiveFrom(), request.effectiveTo(), null))
                .thenReturn(true);

        assertThrows(
                FeeConfigPeriodOverlapException.class,
                () -> service.create(request, authentication));
        verify(feeConfigs, never()).save(any());
    }

    @Test
    void feeConfigAllowsNonOverlappingVersionAndUsesActor() {
        FeeConfigService service = new FeeConfigServiceImpl(feeConfigs, users);
        FeeConfigCreateRequest request =
                new FeeConfigCreateRequest(
                        "ELEC",
                        "Electricity",
                        "kWh",
                        new BigDecimal("3500.00"),
                        FeePricingMode.PER_USAGE,
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 6, 30),
                        true);
        FeeConfigEntity saved =
                FeeConfigEntity.create(
                        "ELEC",
                        "Electricity",
                        "kWh",
                        request.unitPrice(),
                        "PER_USAGE",
                        request.effectiveFrom(),
                        request.effectiveTo(),
                        true,
                        actor);
        when(feeConfigs.existsOverlappingPeriod(
                        "ELEC", request.effectiveFrom(), request.effectiveTo(), null))
                .thenReturn(false);
        when(feeConfigs.save(any(FeeConfigEntity.class))).thenReturn(saved);

        assertEquals("ELEC", service.create(request, authentication).feeCode());
        verify(feeConfigs).save(any(FeeConfigEntity.class));
    }

    @Test
    void feeConfigRejectsInvalidDateRange() {
        FeeConfigService service = new FeeConfigServiceImpl(feeConfigs, users);
        FeeConfigUpdateRequest request =
                new FeeConfigUpdateRequest(
                        "Electricity",
                        "kWh",
                        BigDecimal.ONE,
                        FeePricingMode.PER_USAGE,
                        LocalDate.of(2026, 2, 1),
                        LocalDate.of(2026, 1, 31),
                        true);

        assertThrows(
                InvalidFeeConfigPeriodException.class,
                () -> service.update(1L, request, authentication));
        verify(feeConfigs, never()).findByIdForUpdate(any());
    }

    @Test
    void serviceRejectsDuplicateCodeAndNegativePrice() {
        ServiceManagementService service = new ServiceManagementServiceImpl(services, users);
        ServiceCreateRequest duplicate =
                new ServiceCreateRequest(
                        "WIFI", "WiFi", FeePricingMode.PER_ROOM, BigDecimal.TEN, true);
        when(services.existsByServiceCodeIgnoreCase("WIFI")).thenReturn(true);
        assertThrows(
                DuplicateServiceCodeException.class,
                () -> service.create(duplicate, authentication));

        ServiceCreateRequest negative =
                new ServiceCreateRequest(
                        "WATER", "Water", FeePricingMode.PER_USAGE, new BigDecimal("-1"), true);
        when(services.existsByServiceCodeIgnoreCase("WATER")).thenReturn(false);
        assertThrows(
                InvalidFeePriceException.class, () -> service.create(negative, authentication));
    }

    @Test
    void utilityReadingRejectsEndBeforeStart() {
        UtilityReadingService service = new UtilityReadingServiceImpl(readings, rooms, users);
        when(rooms.findByIdForUpdate(5L)).thenReturn(Optional.of(room));
        UtilityReadingCreateRequest request =
                new UtilityReadingCreateRequest(
                        5L,
                        UtilityType.WATER,
                        "2026-01",
                        new BigDecimal("10.000"),
                        new BigDecimal("9.000"),
                        LocalDate.of(2026, 1, 31),
                        null);

        assertThrows(
                InvalidUtilityReadingException.class,
                () -> service.create(request, authentication));
        verify(readings, never()).save(any());
    }

    @Test
    void utilityReadingRequiresPreviousEndAsNextStart() {
        UtilityReadingService service = new UtilityReadingServiceImpl(readings, rooms, users);
        when(rooms.findByIdForUpdate(5L)).thenReturn(Optional.of(room));
        when(readings.existsByRoom_IdAndUtilityTypeAndBillingPeriod(5L, "WATER", "2026-02"))
                .thenReturn(false);
        when(readings.findPreviousOne(5L, "WATER", "2026-02")).thenReturn(Optional.of(previous));
        when(previous.getReadingEnd()).thenReturn(new BigDecimal("20.000"));
        UtilityReadingCreateRequest request =
                new UtilityReadingCreateRequest(
                        5L,
                        UtilityType.WATER,
                        "2026-02",
                        new BigDecimal("19.000"),
                        new BigDecimal("21.000"),
                        LocalDate.of(2026, 2, 28),
                        null);

        assertThrows(
                UtilityReadingSequenceException.class,
                () -> service.create(request, authentication));
        verify(readings, never()).save(any());
    }

    @Test
    void utilityReadingCannotBreakContinuityWithNextPeriod() {
        UtilityReadingService service = new UtilityReadingServiceImpl(readings, rooms, users);
        when(readings.findByIdForUpdate(9L)).thenReturn(Optional.of(lockedReading));
        when(lockedReading.isLocked()).thenReturn(false);
        when(lockedReading.getRoom()).thenReturn(room);
        when(room.getId()).thenReturn(5L);
        when(lockedReading.getUtilityType()).thenReturn("WATER");
        when(lockedReading.getBillingPeriod()).thenReturn("2026-01");
        when(readings.findPreviousOne(5L, "WATER", "2026-01")).thenReturn(Optional.empty());
        when(readings.findNextOne(5L, "WATER", "2026-01")).thenReturn(Optional.of(next));
        when(next.getReadingStart()).thenReturn(new BigDecimal("20.000"));
        UtilityReadingUpdateRequest request =
                new UtilityReadingUpdateRequest(
                        new BigDecimal("10.000"),
                        new BigDecimal("19.000"),
                        LocalDate.of(2026, 1, 31),
                        null);

        assertThrows(
                UtilityReadingSequenceException.class,
                () -> service.update(9L, request, authentication));
        verify(lockedReading, never()).updateDetails(any(), any(), any(), any());
    }

    @Test
    void utilityReadingCanBeRecordedForEmptyRoomWithoutCreatingFee() {
        UtilityReadingService service = new UtilityReadingServiceImpl(readings, rooms, users);
        when(room.getId()).thenReturn(5L);
        when(rooms.findByIdForUpdate(5L)).thenReturn(Optional.of(room));
        when(readings.existsByRoom_IdAndUtilityTypeAndBillingPeriod(5L, "ELECTRICITY", "2026-01"))
                .thenReturn(false);
        when(readings.findPreviousOne(5L, "ELECTRICITY", "2026-01")).thenReturn(Optional.empty());
        UtilityReadingCreateRequest request =
                new UtilityReadingCreateRequest(
                        5L,
                        UtilityType.ELECTRICITY,
                        "2026-01",
                        BigDecimal.ZERO,
                        new BigDecimal("12.500"),
                        LocalDate.of(2026, 1, 31),
                        "empty room");
        UtilityReadingEntity saved =
                UtilityReadingEntity.create(
                        room,
                        "ELECTRICITY",
                        "2026-01",
                        BigDecimal.ZERO,
                        new BigDecimal("12.500"),
                        request.readingDate(),
                        request.note(),
                        actor);
        when(readings.save(any(UtilityReadingEntity.class))).thenReturn(saved);

        assertEquals(
                new BigDecimal("12.500"), service.create(request, authentication).consumption());
        verify(readings).save(any(UtilityReadingEntity.class));
    }

    @Test
    void lockedReadingCannotBeUpdatedButCanBeLockedSafely() {
        UtilityReadingService service = new UtilityReadingServiceImpl(readings, rooms, users);
        when(readings.findByIdForUpdate(8L)).thenReturn(Optional.of(lockedReading));
        when(lockedReading.isLocked()).thenReturn(true);
        UtilityReadingUpdateRequest update =
                new UtilityReadingUpdateRequest(
                        BigDecimal.ONE, BigDecimal.TEN, LocalDate.of(2026, 1, 31), null);

        assertThrows(
                LockedUtilityReadingException.class,
                () -> service.update(8L, update, authentication));
        assertThrows(
                LockedUtilityReadingException.class,
                () ->
                        service.lock(
                                8L,
                                new com.quanlynhatro.utility.api.UtilityReadingLockRequest(false),
                                authentication));
        verify(readings, never()).save(any());
        verify(readings, org.mockito.Mockito.times(2)).findByIdForUpdate(eq(8L));
    }
}
