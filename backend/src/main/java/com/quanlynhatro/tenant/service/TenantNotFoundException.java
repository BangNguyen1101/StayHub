package com.quanlynhatro.tenant.service;

public class TenantNotFoundException extends RuntimeException {
    public TenantNotFoundException(Long userId) {
        super("Tenant not found: " + userId);
    }

    public TenantNotFoundException(String identifier) {
        super("Tenant not found: " + identifier);
    }
}
