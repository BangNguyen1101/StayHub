package com.quanlynhatro.contract.api;

import jakarta.validation.constraints.Size;

public record ContractCancelRequest(@Size(max = 2000) String note) {}
