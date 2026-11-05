package com.quanlynhatro.utility.repository;

import com.quanlynhatro.utility.domain.FeeConfigEntity;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface FeeConfigRepository extends JpaRepository<FeeConfigEntity, Long> {
    boolean existsByFeeCodeIgnoreCase(String feeCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FeeConfigEntity f where f.id = :id")
    Optional<FeeConfigEntity> findByIdForUpdate(@Param("id") Long id);

    @Query(
            """
            select f from FeeConfigEntity f
            where (:keyword is null or :keyword = ''
              or lower(f.feeCode) like lower(concat('%', :keyword, '%'))
              or lower(f.feeName) like lower(concat('%', :keyword, '%')))
              and (:pricingMode is null or f.pricingMode = :pricingMode)
              and (:active is null or f.active = :active)
            """)
    Page<FeeConfigEntity> search(
            @Param("keyword") String keyword,
            @Param("pricingMode") String pricingMode,
            @Param("active") Boolean active,
            Pageable pageable);

    @Query(
            """
            select f from FeeConfigEntity f
            where lower(f.feeCode) = lower(:feeCode)
              and f.effectiveFrom <= :atDate
              and (f.effectiveTo is null or f.effectiveTo >= :atDate)
              and f.active = true
            order by f.effectiveFrom desc
            """)
    Optional<FeeConfigEntity> findEffective(
            @Param("feeCode") String feeCode, @Param("atDate") LocalDate atDate);

    @Query(
            """
            select case when count(f) > 0 then true else false end
            from FeeConfigEntity f
            where lower(f.feeCode) = lower(:feeCode)
              and (:excludeId is null or f.id <> :excludeId)
              and f.effectiveFrom <= coalesce(:effectiveTo, f.effectiveFrom)
              and (:effectiveFrom <= coalesce(f.effectiveTo, :effectiveFrom)
                   or :effectiveTo is null)
            """)
    boolean existsOverlappingPeriod(
            @Param("feeCode") String feeCode,
            @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("effectiveTo") LocalDate effectiveTo,
            @Param("excludeId") Long excludeId);
}
