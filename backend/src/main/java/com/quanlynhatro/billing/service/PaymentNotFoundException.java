package com.quanlynhatro.billing.service;

public class PaymentNotFoundException extends BillingBusinessException {
    public PaymentNotFoundException(Long id) {
        super("Payment not found: " + id);
    }
}
