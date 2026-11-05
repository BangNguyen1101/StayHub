package com.quanlynhatro.maintenance.repository;

import com.quanlynhatro.maintenance.domain.MaintenanceRequestHistoryEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceRequestHistoryRepository
        extends JpaRepository<MaintenanceRequestHistoryEntity, Long> {
    List<MaintenanceRequestHistoryEntity> findByRequest_IdOrderByChangedAtAsc(Long requestId);
}
