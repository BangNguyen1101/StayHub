package com.quanlynhatro.billing.service;

import com.quanlynhatro.billing.api.*;

import org.springframework.data.domain.*;

public interface BillingService {
    InvoiceResponse createDraft(InvoiceCreateRequest request, Long adminId);

    InvoiceResponse issue(Long id, Long adminId);

    InvoiceResponse cancel(Long id, Long adminId);

    void delete(Long id, Long adminId);

    InvoiceResponse getAdmin(Long id);

    InvoiceResponse getTenant(Long id, Long tenantId);

    /** Invoices issued to a tenant; drafts stay hidden until the admin issues them. */
    Page<InvoiceResponse> searchForTenant(Long tenantId, Pageable pageable);

    /** Issues all draft invoices (of one billing period when given) and returns how many. */
    int issueDrafts(String period, Long adminId);

    Page<InvoiceResponse> search(
            String keyword,
            String period,
            InvoiceStatus status,
            Long contractId,
            Long payerId,
            Pageable pageable);

    Page<DebtResponse> debts(String keyword, Long payerId, Pageable pageable);
}
