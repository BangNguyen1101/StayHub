package com.quanlynhatro.billing.service;

import com.quanlynhatro.billing.api.*;

import org.springframework.data.domain.*;

public interface PaymentService {
    PaymentResponse recordManual(Long invoiceId, ManualPaymentRequest request, Long adminId);

    PaymentQrResponse qrForTenant(Long invoiceId, Long tenantId);

    Page<PaymentResponse> searchAdmin(PaymentQuery query, Pageable pageable);

    Page<PaymentResponse> searchTenant(Long tenantId, Pageable pageable);

    Page<PaymentResponse> invoiceHistory(
            Long invoiceId, Long tenantId, boolean admin, Pageable pageable);
}
