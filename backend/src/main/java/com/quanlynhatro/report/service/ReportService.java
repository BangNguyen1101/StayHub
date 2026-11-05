package com.quanlynhatro.report.service;

import com.quanlynhatro.report.api.ReportDtos;

import java.time.LocalDate;

public interface ReportService {
    ReportDtos.Dashboard dashboard(LocalDate asOf);

    ReportDtos.ReportBundle revenue(LocalDate from, LocalDate to);

    java.util.List<ReportDtos.RoomRevenue> revenueByRoom(LocalDate from, LocalDate to);

    ReportDtos.ReportBundle invoiced(LocalDate from, LocalDate to);

    ReportDtos.Debt debt(LocalDate asOf);

    ReportDtos.ReportBundle invoiceItems(LocalDate from, LocalDate to);

    java.util.List<ReportDtos.Count> rooms();

    java.util.List<ReportDtos.Count> contracts();

    java.util.List<ReportDtos.Count> maintenance();

    ReportDtos.ReportBundle utilities(String fromPeriod, String toPeriod);
}
