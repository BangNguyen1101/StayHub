package com.quanlynhatro.contract.service;

import com.quanlynhatro.contract.api.ContractCancelRequest;
import com.quanlynhatro.contract.api.ContractCreateRequest;
import com.quanlynhatro.contract.api.ContractHistoryResponse;
import com.quanlynhatro.contract.api.ContractRenewRequest;
import com.quanlynhatro.contract.api.ContractResponse;
import com.quanlynhatro.contract.api.ContractStatus;
import com.quanlynhatro.contract.api.ContractSummaryResponse;
import com.quanlynhatro.contract.api.ContractTerminateRequest;
import com.quanlynhatro.contract.api.ContractUpdateRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ContractManagementService {
    Page<ContractSummaryResponse> search(
            String keyword,
            ContractStatus status,
            Long tenantUserId,
            Long roomId,
            Pageable pageable);

    ContractResponse getById(Long contractId);

    ContractResponse getForTenant(String authenticatedUsername, Long contractId);

    List<ContractHistoryResponse> history(Long contractId);

    ContractResponse create(ContractCreateRequest request, Long adminUserId);

    ContractResponse updateDraft(Long contractId, ContractUpdateRequest request, Long adminUserId);

    ContractResponse submitPending(Long contractId, Long adminUserId);

    ContractResponse activate(Long contractId, Long adminUserId);

    ContractResponse renew(Long contractId, ContractRenewRequest request, Long adminUserId);

    ContractResponse terminate(Long contractId, ContractTerminateRequest request, Long adminUserId);

    ContractResponse cancel(Long contractId, ContractCancelRequest request, Long adminUserId);

    void deleteOldContract(Long contractId, Long adminUserId);

    void processScheduledExpiry(LocalDate today);
}
