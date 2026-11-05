package com.quanlynhatro.room.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RoomResponse(
        Long id,
        String roomCode,
        String roomName,
        Integer floorNumber,
        BigDecimal areaM2,
        BigDecimal baseRent,
        Integer maxOccupants,
        boolean allowsSharing,
        String status,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
