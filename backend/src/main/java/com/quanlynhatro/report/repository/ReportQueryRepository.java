package com.quanlynhatro.report.repository;

import com.quanlynhatro.billing.domain.InvoiceEntity;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ReportQueryRepository extends Repository<InvoiceEntity, Long> {
    interface CountRow {
        String getCode();

        Long getCount();
    }

    interface MoneyRow {
        String getCode();

        BigDecimal getAmount();
    }

    interface RoomRevenueRow {
        Long getRoomId();

        String getRoomCode();

        BigDecimal getAmount();
    }

    interface DebtRow {
        Long getInvoiceCount();

        Long getOverdueInvoiceCount();

        BigDecimal getOutstanding();

        BigDecimal getOverdue();
    }

    interface DashboardRow {
        Long getTotalRooms();

        Long getOccupiedRooms();

        Long getVacantRooms();

        Long getActiveContracts();

        Long getExpiringContracts();

        Long getExpiredContracts();

        Long getActiveTenants();

        Long getPendingRentalRequests();

        Long getUnpaidInvoices();

        Long getOverdueInvoices();

        BigDecimal getOutstanding();
    }

    @Query(
            value =
                    """
select count(*) totalRooms,
  sum(case when Status in ('OCCUPIED','FULL') then 1 else 0 end) occupiedRooms,
  sum(case when Status='VACANT' then 1 else 0 end) vacantRooms,
  (select count(*) from Contracts where Status='ACTIVE') activeContracts,
  (select count(*) from Contracts where Status='EXPIRING') expiringContracts,
  (select count(*) from Contracts where Status='EXPIRED') expiredContracts,
  (select count(*) from AppUsers u join Roles r on r.RoleId=u.RoleId where r.RoleCode='TENANT' and u.IsActive=1) activeTenants,
  (select count(*) from RentalRequests where Status in ('PENDING','NEEDS_INFO')) pendingRentalRequests,
  (select count(*) from Invoices where Status in ('DRAFT','UNPAID','PARTIALLY_PAID','OVERDUE')) unpaidInvoices,
  (select count(*) from Invoices where Status='OVERDUE' or (Status in ('UNPAID','PARTIALLY_PAID') and DueDate < :asOf)) overdueInvoices,
  (select coalesce(sum(i.TotalAmount - coalesce(p.PaidAmount,0)),0)
     from Invoices i left join (select InvoiceId, sum(Amount) PaidAmount from Payments where Status='SUCCEEDED' group by InvoiceId) p
       on p.InvoiceId=i.InvoiceId where i.Status not in ('DRAFT','CANCELLED')) outstanding
from Rooms where Status <> 'INACTIVE'
""",
            nativeQuery = true)
    DashboardRow dashboard(@Param("asOf") LocalDate asOf);

    @Query(
            value = "select Status code, count(*) count from Rooms group by Status order by Status",
            nativeQuery = true)
    List<CountRow> roomStatuses();

    @Query(
            value =
                    "select Status code, count(*) count from Contracts group by Status order by"
                        + " Status",
            nativeQuery = true)
    List<CountRow> contractStatuses();

    @Query(
            value =
                    "select Status code, count(*) count from MaintenanceRequests group by Status"
                        + " order by Status",
            nativeQuery = true)
    List<CountRow> maintenanceStatuses();

    @Query(
            value =
                    """
select convert(char(7), (p.ConfirmedAt at time zone 'UTC' at time zone 'SE Asia Standard Time'), 120) code,
       coalesce(sum(p.Amount),0) amount
from Payments p
where p.Status='SUCCEEDED' and p.ConfirmedAt >= :fromAt and p.ConfirmedAt < :toAt
group by convert(char(7), (p.ConfirmedAt at time zone 'UTC' at time zone 'SE Asia Standard Time'), 120)
order by code
""",
            nativeQuery = true)
    List<MoneyRow> revenueByMonth(
            @Param("fromAt") LocalDateTime fromAt, @Param("toAt") LocalDateTime toAt);

    @Query(
            value =
                    """
select r.RoomId roomId, r.RoomCode roomCode, coalesce(sum(p.Amount),0) amount
from Payments p
join Invoices i on i.InvoiceId=p.InvoiceId
join Rooms r on r.RoomId=i.RoomId
where p.Status='SUCCEEDED' and p.ConfirmedAt >= :fromAt and p.ConfirmedAt < :toAt
group by r.RoomId, r.RoomCode
order by r.RoomCode
""",
            nativeQuery = true)
    List<RoomRevenueRow> revenueByRoom(
            @Param("fromAt") LocalDateTime fromAt, @Param("toAt") LocalDateTime toAt);

    @Query(
            value =
                    """
select i.BillingPeriod code, coalesce(sum(i.TotalAmount),0) amount
from Invoices i where i.Status not in ('DRAFT','CANCELLED') and i.IssueDate >= :fromDate and i.IssueDate < :toDate
group by i.BillingPeriod order by i.BillingPeriod
""",
            nativeQuery = true)
    List<MoneyRow> invoicedByPeriod(
            @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Query(
            value =
                    """
select count(*) invoiceCount,
  sum(case when i.Status='OVERDUE' or (i.Status in ('UNPAID','PARTIALLY_PAID') and i.DueDate < :asOf) then 1 else 0 end) overdueInvoiceCount,
  coalesce(sum(i.TotalAmount - coalesce(p.PaidAmount,0)),0) outstanding,
  coalesce(sum(case when i.Status='OVERDUE' or (i.Status in ('UNPAID','PARTIALLY_PAID') and i.DueDate < :asOf)
            then i.TotalAmount - coalesce(p.PaidAmount,0) else 0 end),0) overdue
from Invoices i left join (select InvoiceId, sum(Amount) PaidAmount from Payments where Status='SUCCEEDED' group by InvoiceId) p
  on p.InvoiceId=i.InvoiceId where i.Status not in ('DRAFT','CANCELLED')
""",
            nativeQuery = true)
    DebtRow debt(@Param("asOf") LocalDate asOf);

    @Query(
            value =
                    """
select ii.ItemType code, coalesce(sum(ii.Amount),0) amount
from InvoiceItems ii join Invoices i on i.InvoiceId=ii.InvoiceId
where i.Status not in ('DRAFT','CANCELLED') and i.IssueDate >= :fromDate and i.IssueDate < :toDate
group by ii.ItemType order by ii.ItemType
""",
            nativeQuery = true)
    List<MoneyRow> invoiceItemsByType(
            @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Query(
            value =
                    """
select ur.UtilityType code, coalesce(sum(ur.ReadingEnd - ur.ReadingStart),0) amount
from UtilityReadings ur where ur.BillingPeriod >= :fromPeriod and ur.BillingPeriod <= :toPeriod
group by ur.UtilityType order by ur.UtilityType
""",
            nativeQuery = true)
    List<MoneyRow> utilityConsumption(
            @Param("fromPeriod") String fromPeriod, @Param("toPeriod") String toPeriod);
}
