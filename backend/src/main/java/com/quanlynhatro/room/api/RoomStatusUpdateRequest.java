package com.quanlynhatro.room.api;

import jakarta.validation.constraints.NotNull;

public record RoomStatusUpdateRequest(@NotNull RoomStatus status) {}
