package com.quanlynhatro.tenant.service;

public class TenantProfileNotFoundException extends RuntimeException {
    public TenantProfileNotFoundException(Long userId) {
        super("Tenant profile not found: " + userId);
    }
}
