package com.quanlynhatro.billing.service;

public class InvoiceAccessDeniedException extends BillingBusinessException {
    public InvoiceAccessDeniedException() {
        super("Invoice access denied");
    }
}
