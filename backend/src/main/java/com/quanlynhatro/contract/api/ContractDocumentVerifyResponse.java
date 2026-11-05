package com.quanlynhatro.contract.api;

import java.time.LocalDateTime;

/**
 * Result of comparing a PDF (the stored copy, or a file uploaded for checking) with the SHA-256
 * recorded when the document was signed.
 */
public record ContractDocumentVerifyResponse(
        Long documentId,
        boolean valid,
        String expectedHash,
        String actualHash,
        LocalDateTime checkedAt) {}
