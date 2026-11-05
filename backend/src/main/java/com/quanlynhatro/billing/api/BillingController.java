package com.quanlynhatro.billing.api;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.billing.service.*;
import com.quanlynhatro.room.api.PageResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BillingController {
    private final BillingService billing;
    private final AppUserRepository users;

    public BillingController(BillingService billing, AppUserRepository users) {
        this.billing = billing;
        this.users = users;
    }

    @GetMapping("/admin/invoices")
    public PageResponse<InvoiceResponse> adminSearch(
            Authentication a,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String billingPeriod,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) Long contractId,
            @RequestParam(required = false) Long payerUserId,
            Pageable pageable) {
        admin(a);
        return PageResponse.from(
                billing.search(keyword, billingPeriod, status, contractId, payerUserId, pageable));
    }

    @GetMapping("/admin/invoices/{id}")
    public InvoiceResponse adminGet(Authentication a, @PathVariable @Positive Long id) {
        admin(a);
        return billing.getAdmin(id);
    }

    @PostMapping("/admin/invoices")
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse create(Authentication a, @Valid @RequestBody InvoiceCreateRequest r) {
        return billing.createDraft(r, admin(a).getId());
    }

    @PatchMapping("/admin/invoices/{id}/issue")
    public InvoiceResponse issue(Authentication a, @PathVariable @Positive Long id) {
        return billing.issue(id, admin(a).getId());
    }

    /** Issues every draft invoice, optionally only those of one billing period (yyyy-MM). */
    @PatchMapping("/admin/invoices/issue-drafts")
    public java.util.Map<String, Integer> issueDrafts(
            Authentication a,
            @RequestParam(required = false)
                    @jakarta.validation.constraints.Pattern(regexp = "\\d{4}-\\d{2}")
                    String period) {
        return java.util.Map.of("issued", billing.issueDrafts(period, admin(a).getId()));
    }

    @DeleteMapping("/admin/invoices/{id}")
    public void delete(Authentication a, @PathVariable @Positive Long id) {
        billing.delete(id, admin(a).getId());
    }

    @PatchMapping("/admin/invoices/{id}/cancel")
    public InvoiceResponse cancel(Authentication a, @PathVariable @Positive Long id) {
        return billing.cancel(id, admin(a).getId());
    }

    @GetMapping("/admin/debts")
    public PageResponse<DebtResponse> adminDebts(
            Authentication a, @RequestParam(required = false) String keyword, Pageable pageable) {
        admin(a);
        return PageResponse.from(billing.debts(keyword, null, pageable));
    }

    @GetMapping("/tenant/invoices")
    public PageResponse<InvoiceResponse> tenantInvoices(Authentication a, Pageable pageable) {
        AppUserEntity u = current(a);
        return PageResponse.from(billing.searchForTenant(u.getId(), pageable));
    }

    @GetMapping("/tenant/invoices/{id}")
    public InvoiceResponse tenantInvoice(Authentication a, @PathVariable @Positive Long id) {
        return billing.getTenant(id, current(a).getId());
    }

    @GetMapping("/tenant/debts")
    public PageResponse<DebtResponse> tenantDebts(Authentication a, Pageable pageable) {
        return PageResponse.from(billing.debts(null, current(a).getId(), pageable));
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
