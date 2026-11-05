package com.quanlynhatro.report;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.quanlynhatro.report.repository.ReportQueryRepository;
import com.quanlynhatro.report.service.ReportBusinessException;
import com.quanlynhatro.report.service.ReportServiceImpl;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

class ReportServiceTest {
    private final ReportQueryRepository repository = mock(ReportQueryRepository.class);
    private final ReportServiceImpl service = new ReportServiceImpl(repository);

    @Test
    void dashboardUsesRoomStatusAndReturnsZeroSafeValues() {
        var row = mock(ReportQueryRepository.DashboardRow.class);
        when(row.getTotalRooms()).thenReturn(10L);
        when(row.getOccupiedRooms()).thenReturn(4L);
        when(row.getVacantRooms()).thenReturn(6L);
        when(row.getOutstanding()).thenReturn(null);
        when(repository.dashboard(any())).thenReturn(row);
        when(repository.maintenanceStatuses()).thenReturn(List.of());
        var result = service.dashboard(LocalDate.of(2026, 1, 1));
        assertEquals(10, result.totalRooms());
        assertEquals(new BigDecimal("40.00"), result.occupancyRate());
        assertEquals(BigDecimal.ZERO, result.outstandingDebt());
    }

    @Test
    void emptyRevenueIsReturnedWithoutLoadingPaymentEntities() {
        when(repository.revenueByMonth(any(), any())).thenReturn(List.of());
        assertTrue(
                service.revenue(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
                        .rows()
                        .isEmpty());
        verify(repository).revenueByMonth(any(), any());
    }

    @Test
    void invalidDateRangeIsRejected() {
        assertThrows(
                ReportBusinessException.class,
                () -> service.invoiced(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1)));
        verifyNoInteractions(repository);
    }
}
