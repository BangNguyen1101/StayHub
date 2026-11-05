package com.quanlynhatro.contract.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param signatureImage hand-drawn signature as a PNG data URL ({@code data:image/png;base64,...})
 * @param agreed the tenant confirmed having read and agreed to the document
 */
public record ContractSignRequest(
        @NotBlank @Size(max = 700_000) String signatureImage,
        @AssertTrue(message = "Bạn cần xác nhận đã đọc và đồng ý với nội dung") boolean agreed) {}
