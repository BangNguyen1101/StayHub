package com.quanlynhatro.room.api;

import java.time.LocalDate;

public record RoomOccupantResponse(
        Long occupantId,
        Long userId,
        String fullName,
        String phoneNumber,
        String identityDocumentNumber,
        Long contractId,
        String contractCode,
        boolean primaryTenant,
        LocalDate moveInDate,
        LocalDate moveOutDate) {}
