package com.quanlynhatro.billing.api;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.billing.service.*;
import com.quanlynhatro.room.api.PageResponse;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PaymentController {
    private final PaymentService payments;
    private final AppUserRepository users;

    public PaymentController(PaymentService payments, AppUserRepository users) {
        this.payments = payments;
        this.users = users;
    }

    @PostMapping("/admin/invoices/{invoiceId}/payments/manual")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse manual(
            Authentication a,
            @PathVariable Long invoiceId,
            @Valid @RequestBody ManualPaymentRequest request) {
        return payments.recordManual(invoiceId, request, admin(a).getId());
    }

    @GetMapping("/admin/payments")
    public PageResponse<PaymentResponse> adminSearch(
            Authentication a,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String providerCode,
            @RequestParam(required = false) PaymentStatus status,
            Pageable pageable) {
        admin(a);
        return PageResponse.from(
                payments.searchAdmin(new PaymentQuery(keyword, providerCode, status), pageable));
    }

    @GetMapping("/admin/invoices/{invoiceId}/payments")
    public PageResponse<PaymentResponse> adminHistory(
            Authentication a, @PathVariable Long invoiceId, Pageable pageable) {
        admin(a);
        return PageResponse.from(payments.invoiceHistory(invoiceId, null, true, pageable));
    }

    @GetMapping("/tenant/payments")
    public PageResponse<PaymentResponse> tenantSearch(Authentication a, Pageable pageable) {
        return PageResponse.from(payments.searchTenant(current(a).getId(), pageable));
    }

    @GetMapping("/tenant/invoices/{invoiceId}/payments")
    public PageResponse<PaymentResponse> tenantHistory(
            Authentication a, @PathVariable Long invoiceId, Pageable pageable) {
        return PageResponse.from(
                payments.invoiceHistory(invoiceId, current(a).getId(), false, pageable));
    }

    @GetMapping("/tenant/invoices/{invoiceId}/payment-qr")
    public PaymentQrResponse qr(Authentication a, @PathVariable Long invoiceId) {
        return payments.qrForTenant(invoiceId, current(a).getId());
    }

    private AppUserEntity admin(Authentication a) {
        AppUserEntity u = current(a);
        if (u.getRole() == null || !"ADMIN".equals(u.getRole().getCode()))
            throw new InvoiceAccessDeniedException();
        return u;
    }

    private AppUserEntity current(Authentication a) {
        if (a == null || !a.isAuthenticated() || a.getName() == null)
            throw new InvoiceAccessDeniedException();
        return users.findByUsernameIgnoreCaseOrEmailIgnoreCase(a.getName(), a.getName())
                .orElseThrow(InvoiceAccessDeniedException::new);
    }
}
