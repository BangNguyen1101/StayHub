package com.quanlynhatro.contract.scheduler;

import com.quanlynhatro.contract.service.ContractExpiryRunService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class ContractExpiryScheduler {
    private final ContractExpiryRunService expiryRun;
    private final Clock clock;
    private final boolean enabled;

    @Autowired
    public ContractExpiryScheduler(
            ContractExpiryRunService expiryRun,
            @Value("${app.scheduler.contract-expiry.enabled:false}") boolean enabled) {
        this(expiryRun, Clock.system(ZoneId.of("Asia/Ho_Chi_Minh")), enabled);
    }

    public ContractExpiryScheduler(
            ContractExpiryRunService expiryRun, Clock clock, boolean enabled) {
        this.expiryRun = expiryRun;
        this.clock = clock;
        this.enabled = enabled;
    }

    @Scheduled(
            cron = "${app.scheduler.contract-expiry.cron:0 0 1 * * *}",
            zone = "${app.scheduler.contract-expiry.timezone:Asia/Ho_Chi_Minh}")
    public void processDailyExpiry() {
        if (enabled) expiryRun.run(LocalDate.now(clock));
    }
}
