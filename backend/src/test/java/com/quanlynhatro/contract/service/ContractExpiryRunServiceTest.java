package com.quanlynhatro.contract.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.contract.domain.ScheduledJobRunEntity;
import com.quanlynhatro.contract.repository.ContractHistoryRepository;
import com.quanlynhatro.contract.repository.ContractOccupantRepository;
import com.quanlynhatro.contract.repository.ContractRepository;
import com.quanlynhatro.contract.repository.ScheduledJobRunRepository;
import com.quanlynhatro.contract.repository.SystemConfigRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ContractExpiryRunServiceTest {
    @Mock private ContractRepository contracts;
    @Mock private ContractOccupantRepository occupants;
    @Mock private ContractHistoryRepository histories;
    @Mock private com.quanlynhatro.auth.repository.AppUserRepository users;
    @Mock private com.quanlynhatro.room.repository.RoomRepository rooms;
    @Mock private SystemConfigRepository configs;
    @Mock private ScheduledJobRunRepository jobRuns;

    @Test
    void usesConfiguredWarningDaysAndWritesSuccessfulRun() {
        ContractExpiryRunService service = service();
        AppUserEntity admin = org.mockito.Mockito.mock(AppUserEntity.class);
        when(configs.findById("CONTRACT_EXPIRY_WARNING_DAYS")).thenReturn(Optional.empty());
        when(users.findFirstByRole_CodeAndActiveTrueOrderByIdAsc("ADMIN"))
                .thenReturn(Optional.of(admin));
        when(contracts.findByStatusInAndEndDateLessThanEqualOrderByIdAsc(
                        any(), eq(LocalDate.of(2026, 10, 19).plusDays(30))))
                .thenReturn(List.of());
        when(jobRuns.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.run(LocalDate.of(2026, 10, 19));

        verify(contracts)
                .findByStatusInAndEndDateLessThanEqualOrderByIdAsc(
                        any(), eq(LocalDate.of(2026, 10, 19).plusDays(30)));
        verify(jobRuns, org.mockito.Mockito.times(2)).save(any(ScheduledJobRunEntity.class));
    }

    @Test
    void missingActiveAdminProducesFailedRunWithoutProcessingContracts() {
        ContractExpiryRunService service = service();
        when(jobRuns.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(users.findFirstByRole_CodeAndActiveTrueOrderByIdAsc("ADMIN"))
                .thenReturn(Optional.empty());

        service.run(LocalDate.of(2026, 10, 19));

        verify(contracts, never()).findByStatusInAndEndDateLessThanEqualOrderByIdAsc(any(), any());
        verify(jobRuns, org.mockito.Mockito.times(2)).save(any(ScheduledJobRunEntity.class));
    }

    private ContractExpiryRunService service() {
        return new ContractExpiryRunService(
                contracts,
                occupants,
                histories,
                users,
                rooms,
                configs,
                jobRuns,
                new ObjectMapper().findAndRegisterModules());
    }
}
