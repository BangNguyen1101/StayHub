package com.quanlynhatro.contract.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.contract.api.ContractStatus;
import com.quanlynhatro.contract.domain.ContractEntity;
import com.quanlynhatro.contract.domain.ContractHistoryEntity;
import com.quanlynhatro.contract.domain.ScheduledJobRunEntity;
import com.quanlynhatro.contract.repository.ContractHistoryRepository;
import com.quanlynhatro.contract.repository.ContractOccupantRepository;
import com.quanlynhatro.contract.repository.ContractRepository;
import com.quanlynhatro.contract.repository.ScheduledJobRunRepository;
import com.quanlynhatro.contract.repository.SystemConfigRepository;
import com.quanlynhatro.room.api.RoomStatus;
import com.quanlynhatro.room.domain.RoomEntity;
import com.quanlynhatro.room.repository.RoomRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ContractExpiryRunService {
    private static final Logger log = LoggerFactory.getLogger(ContractExpiryRunService.class);
    private static final String JOB_CODE = "CONTRACT_EXPIRY";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final int DEFAULT_WARNING_DAYS = 30;
    private static final List<ContractStatus> EFFECTIVE_STATUSES =
            List.of(ContractStatus.ACTIVE, ContractStatus.EXPIRING);

    private final ContractRepository contracts;
    private final ContractOccupantRepository occupants;
    private final ContractHistoryRepository histories;
    private final AppUserRepository users;
    private final RoomRepository rooms;
    private final SystemConfigRepository configs;
    private final ScheduledJobRunRepository jobRuns;
    private final ObjectMapper objectMapper;

    public ContractExpiryRunService(
            ContractRepository contracts,
            ContractOccupantRepository occupants,
            ContractHistoryRepository histories,
            AppUserRepository users,
            RoomRepository rooms,
            SystemConfigRepository configs,
            ScheduledJobRunRepository jobRuns,
            ObjectMapper objectMapper) {
        this.contracts = contracts;
        this.occupants = occupants;
        this.histories = histories;
        this.users = users;
        this.rooms = rooms;
        this.configs = configs;
        this.jobRuns = jobRuns;
        this.objectMapper = objectMapper;
    }

    public void run(LocalDate today) {
        LocalDateTime startedAt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        ScheduledJobRunEntity run =
                jobRuns.save(ScheduledJobRunEntity.running(JOB_CODE, today.toString(), startedAt));
        int success = 0;
        int skipped = 0;
        int errors = 0;
        String lastError = null;
        try {
            int warningDays = readWarningDays();
            AppUserEntity actor =
                    users.findFirstByRole_CodeAndActiveTrueOrderByIdAsc(ADMIN_ROLE)
                            .orElseThrow(
                                    () ->
                                            new ContractBusinessException(
                                                    "No active ADMIN available for scheduled"
                                                        + " contract processing"));
            List<ContractEntity> candidates =
                    contracts.findByStatusInAndEndDateLessThanEqualOrderByIdAsc(
                            EFFECTIVE_STATUSES, today.plusDays(warningDays));
            for (ContractEntity candidate : candidates) {
                try {
                    Result result = processOne(candidate.getId(), today, actor.getId());
                    if (result == Result.SKIPPED) skipped++;
                    else success++;
                } catch (RuntimeException ex) {
                    errors++;
                    lastError = "Contract " + candidate.getId() + ": " + safeMessage(ex);
                    log.warn(
                            "Scheduled contract expiry processing failed for contract {}",
                            candidate.getId(),
                            ex);
                }
            }
            run.finish(
                    now(),
                    errors == 0 ? "SUCCEEDED" : "PARTIAL",
                    success,
                    skipped,
                    errors,
                    lastError);
        } catch (RuntimeException ex) {
            lastError = safeMessage(ex);
            log.error("Scheduled contract expiry run failed", ex);
            run.finish(now(), "FAILED", success, skipped, 1, lastError);
        }
        jobRuns.save(run);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result processOne(Long contractId, LocalDate today, Long actorId) {
        ContractEntity initial =
                contracts
                        .findById(contractId)
                        .orElseThrow(() -> new ContractNotFoundException(contractId));
        RoomEntity room =
                rooms.findByIdForUpdate(initial.getRoom().getId())
                        .orElseThrow(
                                () ->
                                        new ContractBusinessException(
                                                "Room not found: " + initial.getRoom().getId()));
        ContractEntity contract =
                contracts
                        .findByIdForUpdate(contractId)
                        .orElseThrow(() -> new ContractNotFoundException(contractId));
        if (!EFFECTIVE_STATUSES.contains(contract.getStatus())) return Result.SKIPPED;

        AppUserEntity actor =
                users.findByIdAndRole_Code(actorId, ADMIN_ROLE)
                        .orElseThrow(
                                () ->
                                        new ContractBusinessException(
                                                "Scheduler actor is not an active ADMIN"));
        if (contract.getEndDate().isBefore(today)) {
            String before = snapshot(contract);
            contract.markExpired(contract.getEndDate());
            synchronizeRoom(room, today);
            histories.save(
                    ContractHistoryEntity.create(
                            contract, "EXPIRED", before, snapshot(contract), actor));
            return Result.EXPIRED;
        }
        if (contract.getStatus() == ContractStatus.ACTIVE
                && !contract.getEndDate().isAfter(today.plusDays(readWarningDays()))) {
            String before = snapshot(contract);
            contract.changeStatus(ContractStatus.EXPIRING);
            histories.save(
                    ContractHistoryEntity.create(
                            contract, "MARKED_EXPIRING", before, snapshot(contract), actor));
            return Result.EXPIRING;
        }
        return Result.SKIPPED;
    }

    private int readWarningDays() {
        return configs.findById("CONTRACT_EXPIRY_WARNING_DAYS")
                .map(config -> parseWarningDays(config.getValue()))
                .orElse(DEFAULT_WARNING_DAYS);
    }

    private int parseWarningDays(String value) {
        try {
            int days = Integer.parseInt(value);
            return days >= 0 ? days : DEFAULT_WARNING_DAYS;
        } catch (NumberFormatException ex) {
            return DEFAULT_WARNING_DAYS;
        }
    }

    private void synchronizeRoom(RoomEntity room, LocalDate today) {
        if (room.getStatus().equals(RoomStatus.MAINTENANCE.name())
                || room.getStatus().equals(RoomStatus.INACTIVE.name())) return;
        long count = occupants.countCurrentByRoomId(room.getId(), EFFECTIVE_STATUSES, today);
        if (count == 0) room.changeStatus(RoomStatus.VACANT);
        else if (count >= room.getMaxOccupants()) room.changeStatus(RoomStatus.FULL);
        else room.changeStatus(RoomStatus.OCCUPIED);
    }

    private String snapshot(ContractEntity contract) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("contractId", contract.getId());
        data.put("contractCode", contract.getContractCode());
        data.put("status", contract.getStatus());
        data.put("endDate", contract.getEndDate());
        data.put("actualEndDate", contract.getActualEndDate());
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException ex) {
            throw new ContractBusinessException("Unable to serialize contract history");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    private String safeMessage(RuntimeException ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    public enum Result {
        EXPIRING,
        EXPIRED,
        SKIPPED
    }
}
