package com.quanlynhatro.billing.service;

public class PaymentConflictException extends BillingBusinessException {
    public PaymentConflictException(String message) {
        super(message);
    }
}
