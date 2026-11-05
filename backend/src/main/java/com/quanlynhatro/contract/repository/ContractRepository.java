package com.quanlynhatro.contract.repository;

import com.quanlynhatro.contract.api.ContractStatus;
import com.quanlynhatro.contract.domain.ContractEntity;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ContractRepository extends JpaRepository<ContractEntity, Long> {
    boolean existsByContractCodeIgnoreCase(String contractCode);

    Optional<ContractEntity> findByContractCodeIgnoreCase(String contractCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ContractEntity c where c.id = :id")
    Optional<ContractEntity> findByIdForUpdate(@Param("id") Long id);

    @Query(
            """
            select c from ContractEntity c
            where (:keyword is null or :keyword = ''
                or lower(c.contractCode) like lower(concat('%', :keyword, '%'))
                or lower(c.tenantUser.username) like lower(concat('%', :keyword, '%'))
                or lower(c.tenantUser.fullName) like lower(concat('%', :keyword, '%'))
                or lower(c.room.roomCode) like lower(concat('%', :keyword, '%')))
              and (:status is null or c.status = :status)
              and (:tenantUserId is null or c.tenantUser.id = :tenantUserId)
              and (:roomId is null or c.room.id = :roomId)
            """)
    Page<ContractEntity> search(
            @Param("keyword") String keyword,
            @Param("status") ContractStatus status,
            @Param("tenantUserId") Long tenantUserId,
            @Param("roomId") Long roomId,
            Pageable pageable);

    List<ContractEntity> findByTenantUser_IdAndStatusIn(
            Long tenantUserId, Collection<ContractStatus> statuses);

    List<ContractEntity> findByTenantUser_Id(Long tenantUserId);

    List<ContractEntity> findByRoom_IdAndStatusIn(Long roomId, Collection<ContractStatus> statuses);

    boolean existsByRoom_IdAndStatusIn(Long roomId, Collection<ContractStatus> statuses);

    boolean existsByTenantUser_IdAndStatusIn(
            Long tenantUserId, Collection<ContractStatus> statuses);

    List<ContractEntity> findByStatusInAndEndDateLessThanEqualOrderByIdAsc(
            Collection<ContractStatus> statuses, LocalDate endDate);

    List<ContractEntity> findByStatusInAndEndDateBetweenOrderByEndDateAsc(
            Collection<ContractStatus> statuses, LocalDate from, LocalDate to);
}
