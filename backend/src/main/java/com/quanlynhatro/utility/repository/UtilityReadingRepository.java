package com.quanlynhatro.utility.repository;

import com.quanlynhatro.utility.domain.UtilityReadingEntity;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UtilityReadingRepository extends JpaRepository<UtilityReadingEntity, Long> {
    boolean existsByRoom_IdAndUtilityTypeAndBillingPeriod(
            Long roomId, String utilityType, String billingPeriod);

    Optional<UtilityReadingEntity> findByRoom_IdAndUtilityTypeAndBillingPeriod(
            Long roomId, String utilityType, String billingPeriod);

    @Query(
            """
            select r from UtilityReadingEntity r
            where r.room.id = :roomId
              and r.utilityType = :utilityType
              and r.billingPeriod < :billingPeriod
            order by r.billingPeriod desc
            """)
    Page<UtilityReadingEntity> findPrevious(
            @Param("roomId") Long roomId,
            @Param("utilityType") String utilityType,
            @Param("billingPeriod") String billingPeriod,
            Pageable pageable);

    default Optional<UtilityReadingEntity> findPreviousOne(
            Long roomId, String utilityType, String billingPeriod) {
        return findPrevious(roomId, utilityType, billingPeriod, Pageable.ofSize(1)).stream()
                .findFirst();
    }

    @Query(
            """
            select r from UtilityReadingEntity r
            where r.room.id = :roomId
              and r.utilityType = :utilityType
              and r.billingPeriod > :billingPeriod
            order by r.billingPeriod asc
            """)
    Page<UtilityReadingEntity> findNext(
            @Param("roomId") Long roomId,
            @Param("utilityType") String utilityType,
            @Param("billingPeriod") String billingPeriod,
            Pageable pageable);

    default Optional<UtilityReadingEntity> findNextOne(
            Long roomId, String utilityType, String billingPeriod) {
        return findNext(roomId, utilityType, billingPeriod, Pageable.ofSize(1)).stream()
                .findFirst();
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from UtilityReadingEntity r where r.id = :id")
    Optional<UtilityReadingEntity> findByIdForUpdate(@Param("id") Long id);

    @Query(
            """
            select r from UtilityReadingEntity r
            where (:roomId is null or r.room.id = :roomId)
              and (:utilityType is null or r.utilityType = :utilityType)
              and (:locked is null or r.locked = :locked)
            """)
    Page<UtilityReadingEntity> search(
            @Param("roomId") Long roomId,
            @Param("utilityType") String utilityType,
            @Param("locked") Boolean locked,
            Pageable pageable);
}
