package com.quanlynhatro.contract.service;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.contract.api.ContractDocumentResponse;
import com.quanlynhatro.contract.api.ContractDocumentVerifyResponse;
import com.quanlynhatro.contract.api.ContractSignRequest;
import com.quanlynhatro.contract.domain.ContractEntity;

import java.util.List;

public interface ContractDocumentService {
    /** Who is reading a document: an admin, or a tenant who must belong to the contract. */
    record Viewer(Long userId, boolean admin) {}

    record DocumentPdf(String fileName, byte[] content) {}

    List<ContractDocumentResponse> list(Long contractId, Viewer viewer);

    /** Admin re-sends the signing request for a PENDING contract that has none (idempotent). */
    ContractDocumentResponse issueForAdmin(Long contractId, Long adminUserId);

    /** Issues the lease for signing; the caller must hold the contract lock. */
    void issueContractDocument(ContractEntity contract, AppUserEntity admin);

    ContractDocumentResponse requestRenewal(Long contractId, int months, Long tenantUserId);

    ContractDocumentResponse sign(
            Long documentId,
            ContractSignRequest request,
            Long tenantUserId,
            String signerIp,
            String signerUserAgent);

    DocumentPdf pdf(Long documentId, Viewer viewer);

    ContractDocumentVerifyResponse verifyStored(Long documentId, Viewer viewer);

    ContractDocumentVerifyResponse verifyUpload(Long documentId, byte[] file, Viewer viewer);

    boolean isContractSigned(Long contractId);

    /** Voids a pending signing request when the contract is cancelled or terminated. */
    void voidAwaiting(Long contractId);

    /** Removes unsigned documents before a contract is deleted; signed ones block deletion. */
    void deleteForContract(Long contractId);
}
