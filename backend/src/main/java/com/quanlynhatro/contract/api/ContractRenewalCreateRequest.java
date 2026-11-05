package com.quanlynhatro.contract.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ContractRenewalCreateRequest(@Min(1) @Max(36) int months) {}
