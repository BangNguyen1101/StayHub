package com.quanlynhatro.rental.api;

import java.time.LocalDateTime;

public record RentalRequestHistoryResponse(
        Long id,
        Long rentalRequestId,
        RentalRequestStatus oldStatus,
        RentalRequestStatus newStatus,
        String note,
        Long changedByUserId,
        LocalDateTime changedAt) {}
