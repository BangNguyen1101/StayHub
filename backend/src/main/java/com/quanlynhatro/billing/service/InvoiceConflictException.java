package com.quanlynhatro.billing.service;

public class InvoiceConflictException extends BillingBusinessException {
    public InvoiceConflictException(String message) {
        super(message);
    }
}
