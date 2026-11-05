package com.quanlynhatro.contract.api;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ContractDocumentResponse(
        Long id,
        Long contractId,
        String contractCode,
        ContractDocumentType documentType,
        ContractDocumentStatus status,
        LocalDate previousEndDate,
        LocalDate newEndDate,
        Integer renewalMonths,
        String contentHash,
        Long signerUserId,
        String signerName,
        LocalDateTime signedAt,
        String signerIp,
        String signedPdfHash,
        LocalDateTime createdAt,
        LocalDateTime voidedAt) {}
