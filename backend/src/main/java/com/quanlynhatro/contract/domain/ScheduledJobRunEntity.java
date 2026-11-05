package com.quanlynhatro.contract.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "ScheduledJobRuns", schema = "dbo")
public class ScheduledJobRunEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ScheduledJobRunId", nullable = false)
    private Long id;

    @Column(name = "JobCode", nullable = false, length = 60)
    private String jobCode;

    @Column(name = "BusinessKey", length = 200)
    private String businessKey;

    @Column(name = "StartedAt", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "FinishedAt")
    private LocalDateTime finishedAt;

    @Column(name = "Status", nullable = false, length = 20)
    private String status;

    @Column(name = "SuccessCount", nullable = false)
    private int successCount;

    @Column(name = "SkippedCount", nullable = false)
    private int skippedCount;

    @Column(name = "ErrorCount", nullable = false)
    private int errorCount;

    @Column(name = "ErrorMessage", length = 2000)
    private String errorMessage;

    protected ScheduledJobRunEntity() {}

    public static ScheduledJobRunEntity running(
            String jobCode, String businessKey, LocalDateTime startedAt) {
        ScheduledJobRunEntity run = new ScheduledJobRunEntity();
        run.jobCode = jobCode;
        run.businessKey = businessKey;
        run.startedAt = startedAt;
        run.status = "RUNNING";
        return run;
    }

    public void finish(
            LocalDateTime finishedAt,
            String status,
            int successCount,
            int skippedCount,
            int errorCount,
            String errorMessage) {
        this.finishedAt = finishedAt;
        this.status = status;
        this.successCount = successCount;
        this.skippedCount = skippedCount;
        this.errorCount = errorCount;
        this.errorMessage = errorMessage;
    }

    public Long getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
