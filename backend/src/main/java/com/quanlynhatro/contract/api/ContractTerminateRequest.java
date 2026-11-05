package com.quanlynhatro.contract.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ContractTerminateRequest(
        @NotNull LocalDate actualEndDate, @Size(max = 2000) String note) {}
