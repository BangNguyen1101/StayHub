package com.quanlynhatro.report.service;

import com.quanlynhatro.report.api.ReportDtos;
import com.quanlynhatro.report.repository.ReportQueryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final ReportQueryRepository repository;

    public ReportServiceImpl(ReportQueryRepository repository) {
        this.repository = repository;
    }

    public ReportDtos.Dashboard dashboard(LocalDate asOf) {
        var row = repository.dashboard(asOf == null ? LocalDate.now(ZONE) : asOf);
        long total = nz(row.getTotalRooms());
        long occupied = nz(row.getOccupiedRooms());
        long denominator = total;
        BigDecimal rate =
                denominator == 0
                        ? BigDecimal.ZERO
                        : BigDecimal.valueOf(occupied * 100.0 / denominator)
                                .setScale(2, java.math.RoundingMode.HALF_UP);
        return new ReportDtos.Dashboard(
                total,
                occupied,
                nz(row.getVacantRooms()),
                rate,
                nz(row.getActiveContracts()),
                nz(row.getExpiringContracts()),
                nz(row.getExpiredContracts()),
                nz(row.getActiveTenants()),
                nz(row.getPendingRentalRequests()),
                counts(repository.maintenanceStatuses()),
                nz(row.getUnpaidInvoices()),
                nz(row.getOverdueInvoices()),
                money(row.getOutstanding()));
    }

    public ReportDtos.ReportBundle revenue(LocalDate from, LocalDate to) {
        return moneyBundle(repository.revenueByMonth(fromAt(from), fromAt(to.plusDays(1))));
    }

    public List<ReportDtos.RoomRevenue> revenueByRoom(LocalDate from, LocalDate to) {
        validate(from, to);
        return repository.revenueByRoom(fromAt(from), fromAt(to.plusDays(1))).stream()
                .map(
                        row ->
                                new ReportDtos.RoomRevenue(
                                        nz(row.getRoomId()),
                                        row.getRoomCode(),
                                        money(row.getAmount())))
                .toList();
    }

    public ReportDtos.ReportBundle invoiced(LocalDate from, LocalDate to) {
        validate(from, to);
        return moneyBundle(repository.invoicedByPeriod(from, to.plusDays(1)));
    }

    public ReportDtos.Debt debt(LocalDate asOf) {
        var r = repository.debt(asOf == null ? LocalDate.now(ZONE) : asOf);
        return new ReportDtos.Debt(
                nz(r.getInvoiceCount()),
                nz(r.getOverdueInvoiceCount()),
                money(r.getOutstanding()),
                money(r.getOverdue()));
    }

    public ReportDtos.ReportBundle invoiceItems(LocalDate from, LocalDate to) {
        validate(from, to);
        return moneyBundle(repository.invoiceItemsByType(from, to.plusDays(1)));
    }

    public List<ReportDtos.Count> rooms() {
        return counts(repository.roomStatuses());
    }

    public List<ReportDtos.Count> contracts() {
        return counts(repository.contractStatuses());
    }

    public List<ReportDtos.Count> maintenance() {
        return counts(repository.maintenanceStatuses());
    }

    public ReportDtos.ReportBundle utilities(String from, String to) {
        if (from == null || to == null || from.compareTo(to) > 0)
            throw new ReportBusinessException("Invalid utility period");
        return moneyBundle(repository.utilityConsumption(from, to));
    }

    private LocalDateTime fromAt(LocalDate d) {
        validate(d, d);
        return d.atStartOfDay(ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private void validate(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to))
            throw new ReportBusinessException("Invalid report date range");
    }

    private static long nz(Long v) {
        return v == null ? 0 : v;
    }

    private static BigDecimal money(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static List<ReportDtos.Count> counts(List<ReportQueryRepository.CountRow> rows) {
        return rows.stream().map(r -> new ReportDtos.Count(r.getCode(), nz(r.getCount()))).toList();
    }

    private static ReportDtos.ReportBundle moneyBundle(List<ReportQueryRepository.MoneyRow> rows) {
        return new ReportDtos.ReportBundle(
                rows.stream()
                        .map(r -> new ReportDtos.Money(r.getCode(), money(r.getAmount())))
                        .toList());
    }
}
