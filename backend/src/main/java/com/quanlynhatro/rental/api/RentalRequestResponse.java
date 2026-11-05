package com.quanlynhatro.rental.api;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record RentalRequestResponse(
        Long id,
        String requestCode,
        Long applicantUserId,
        Long roomId,
        String roomCode,
        String contactName,
        String contactPhone,
        String contactEmail,
        Integer expectedOccupantCount,
        LocalDate expectedMoveInDate,
        String note,
        RentalRequestStatus status,
        Long processedByUserId,
        LocalDateTime processedAt,
        String processingNote,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
