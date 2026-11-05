package com.quanlynhatro.rental.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RentalRequestDecisionRequest(
        @NotNull RentalRequestStatus status, @Size(max = 2000) String note) {}
