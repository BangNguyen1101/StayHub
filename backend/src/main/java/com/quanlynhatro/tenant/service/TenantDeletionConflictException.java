package com.quanlynhatro.tenant.service;

public class TenantDeletionConflictException extends RuntimeException {
    public TenantDeletionConflictException() {
        super("Cannot remove a tenant who has an active contract");
    }
}
