package com.quanlynhatro.maintenance.service;

public class MaintenanceAccessDeniedException extends MaintenanceBusinessException {
    public MaintenanceAccessDeniedException() {
        super("Maintenance request access denied");
    }
}
