package com.quanlynhatro.rental.api;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record RentalRequestSummaryResponse(
        Long id,
        String requestCode,
        Long roomId,
        String roomCode,
        String contactName,
        Integer expectedOccupantCount,
        LocalDate expectedMoveInDate,
        RentalRequestStatus status,
        LocalDateTime createdAt) {}
