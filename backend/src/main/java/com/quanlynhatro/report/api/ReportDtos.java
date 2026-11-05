package com.quanlynhatro.report.api;

import java.math.BigDecimal;
import java.util.List;

public final class ReportDtos {
    private ReportDtos() {}

    public record Count(String code, long count) {}

    public record Money(String code, BigDecimal amount) {}

    public record Dashboard(
            long totalRooms,
            long occupiedRooms,
            long vacantRooms,
            BigDecimal occupancyRate,
            long activeContracts,
            long expiringContracts,
            long expiredContracts,
            long activeTenants,
            long pendingRentalRequests,
            List<Count> maintenanceStatuses,
            long unpaidInvoices,
            long overdueInvoices,
            BigDecimal outstandingDebt) {}

    public record Debt(
            long invoiceCount,
            long overdueInvoiceCount,
            BigDecimal outstanding,
            BigDecimal overdue) {}

    public record ReportBundle(List<Money> rows) {}

    public record RoomRevenue(long roomId, String roomCode, BigDecimal amount) {}
}
