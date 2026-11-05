package com.quanlynhatro.billing.api;

public record PaymentQuery(String keyword, String providerCode, PaymentStatus status) {}
