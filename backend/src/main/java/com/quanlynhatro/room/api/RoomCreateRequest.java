package com.quanlynhatro.room.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RoomCreateRequest(
        @NotBlank @Size(max = 50) String roomCode,
        @Size(max = 200) String roomName,
        @NotNull @Positive Integer floorNumber,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 8, fraction = 2) BigDecimal areaM2,
        @NotNull @DecimalMin(value = "0.00") @Digits(integer = 17, fraction = 2)
                BigDecimal baseRent,
        @NotNull @Positive Integer maxOccupants,
        @NotNull Boolean allowsSharing,
        @Size(max = 1000) String notes) {}
