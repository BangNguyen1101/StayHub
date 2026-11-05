package com.quanlynhatro.contract.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.quanlynhatro.contract.scheduler.ContractExpiryScheduler;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

class ContractExpirySchedulerTest {
    @Test
    void runsUsingTheConfiguredLocalDate() {
        ContractExpiryRunService service = mock(ContractExpiryRunService.class);
        Clock clock = Clock.fixed(Instant.parse("2026-10-19T00:00:00Z"), ZoneId.of("UTC"));

        new ContractExpiryScheduler(service, clock, true).processDailyExpiry();

        verify(service).run(LocalDate.of(2026, 10, 19));
    }

    @Test
    void repeatedRunsAreDelegatedToTheIdempotentService() {
        ContractExpiryRunService service = mock(ContractExpiryRunService.class);
        ContractExpiryScheduler scheduler =
                new ContractExpiryScheduler(service, Clock.systemUTC(), true);

        scheduler.processDailyExpiry();
        scheduler.processDailyExpiry();

        verify(service, times(2)).run(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doesNotRunWhenDisabledByDefaultConfiguration() {
        ContractExpiryRunService service = mock(ContractExpiryRunService.class);
        new ContractExpiryScheduler(service, Clock.systemUTC(), false).processDailyExpiry();
        verify(service, org.mockito.Mockito.never()).run(org.mockito.ArgumentMatchers.any());
    }
}
