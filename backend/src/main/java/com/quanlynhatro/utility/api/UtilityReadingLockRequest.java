package com.quanlynhatro.utility.api;

import jakarta.validation.constraints.NotNull;

public record UtilityReadingLockRequest(@NotNull Boolean locked) {}
