package com.quanlynhatro.contract.api;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ContractRenewRequest(@NotNull LocalDate endDate) {}
