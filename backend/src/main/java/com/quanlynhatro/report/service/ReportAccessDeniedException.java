package com.quanlynhatro.report.service;

public class ReportAccessDeniedException extends RuntimeException {
    public ReportAccessDeniedException() {
        super("Admin access required");
    }
}
