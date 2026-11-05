package com.quanlynhatro.billing.service;

public class InvoiceNotFoundException extends BillingBusinessException {
    public InvoiceNotFoundException(Long id) {
        super("Invoice not found: " + id);
    }
}
