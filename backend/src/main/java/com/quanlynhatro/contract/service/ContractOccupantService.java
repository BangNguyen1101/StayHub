package com.quanlynhatro.contract.service;

import com.quanlynhatro.contract.api.ContractOccupantCreateRequest;
import com.quanlynhatro.contract.api.ContractOccupantResponse;
import com.quanlynhatro.contract.api.ContractOccupantUpdateRequest;

import java.time.LocalDate;
import java.util.List;

public interface ContractOccupantService {
    List<ContractOccupantResponse> list(Long contractId);

    ContractOccupantResponse add(
            Long contractId, ContractOccupantCreateRequest request, Long adminUserId);

    ContractOccupantResponse update(
            Long occupantId, ContractOccupantUpdateRequest request, Long adminUserId);

    ContractOccupantResponse moveOut(Long occupantId, LocalDate moveOutDate, Long adminUserId);

    void remove(Long occupantId, Long adminUserId);
}
