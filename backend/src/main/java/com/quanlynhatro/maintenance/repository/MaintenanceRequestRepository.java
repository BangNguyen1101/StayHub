package com.quanlynhatro.maintenance.repository;

import com.quanlynhatro.maintenance.api.*;
import com.quanlynhatro.maintenance.domain.MaintenanceRequestEntity;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface MaintenanceRequestRepository
        extends JpaRepository<MaintenanceRequestEntity, Long> {
    @Modifying
    @Query(
            "update MaintenanceRequestEntity m set m.contract = null where m.contract.id ="
                + " :contractId")
    void clearContractReferences(@Param("contractId") Long contractId);

    boolean existsByRequestCodeIgnoreCase(String requestCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MaintenanceRequestEntity m where m.id = :id")
    Optional<MaintenanceRequestEntity> findByIdForUpdate(@Param("id") Long id);

    @Query(
            """
select m from MaintenanceRequestEntity m
where (:keyword is null or :keyword = '' or lower(m.requestCode) like lower(concat('%', :keyword, '%'))
       or lower(m.title) like lower(concat('%', :keyword, '%')))
  and (:status is null or m.status = :status)
  and (:priority is null or m.priority = :priority)
  and (:issueType is null or m.issueType = :issueType)
  and (:roomId is null or m.room.id = :roomId)
  and (:reporterUserId is null or m.reporterUser.id = :reporterUserId)
  and (:assignedToUserId is null or m.assignedToUser.id = :assignedToUserId)
  and (:createdFrom is null or m.createdAt >= :createdFrom)
  and (:createdTo is null or m.createdAt < :createdTo)
order by m.createdAt desc
""")
    Page<MaintenanceRequestEntity> search(
            @Param("keyword") String keyword,
            @Param("status") MaintenanceStatus status,
            @Param("priority") MaintenancePriority priority,
            @Param("issueType") MaintenanceIssueType issueType,
            @Param("roomId") Long roomId,
            @Param("reporterUserId") Long reporterUserId,
            @Param("assignedToUserId") Long assignedToUserId,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo,
            Pageable pageable);
}
