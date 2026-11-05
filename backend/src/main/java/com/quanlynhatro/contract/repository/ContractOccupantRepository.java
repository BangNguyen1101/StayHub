package com.quanlynhatro.contract.repository;

import com.quanlynhatro.contract.api.ContractStatus;
import com.quanlynhatro.contract.domain.ContractEntity;
import com.quanlynhatro.contract.domain.ContractOccupantEntity;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ContractOccupantRepository extends JpaRepository<ContractOccupantEntity, Long> {
    void deleteByContract_Id(Long contractId);

    boolean existsByContract_IdAndUser_Id(Long contractId, Long userId);

    @Query(
            """
            select count(o) > 0 from ContractOccupantEntity o
            where o.user.id = :userId
              and o.contract.status in :statuses
              and o.moveInDate <= :asOfDate
              and (o.moveOutDate is null or o.moveOutDate >= :asOfDate)
            """)
    boolean existsCurrentByUserId(
            @Param("userId") Long userId,
            @Param("statuses") Collection<ContractStatus> statuses,
            @Param("asOfDate") LocalDate asOfDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from ContractOccupantEntity o where o.id = :id")
    Optional<ContractOccupantEntity> findByIdForUpdate(@Param("id") Long id);

    List<ContractOccupantEntity> findByContract_IdOrderByMoveInDateAsc(Long contractId);

    @Query(
            """
            select o.contract from ContractOccupantEntity o
            where o.user.id = :userId
            order by o.moveInDate desc
            """)
    List<ContractEntity> findContractsByUserId(@Param("userId") Long userId);

    Optional<ContractOccupantEntity> findByContract_IdAndPrimaryTenantTrue(Long contractId);

    long countByContract_IdAndPrimaryTenantTrue(Long contractId);

    @Query(
            """
            select o from ContractOccupantEntity o
            where o.contract.room.id = :roomId
              and o.contract.status in :statuses
              and o.moveInDate <= :asOfDate
              and (o.moveOutDate is null or o.moveOutDate >= :asOfDate)
            order by o.moveInDate asc
            """)
    List<ContractOccupantEntity> findCurrentByRoomId(
            @Param("roomId") Long roomId,
            @Param("statuses") Collection<ContractStatus> statuses,
            @Param("asOfDate") LocalDate asOfDate);

    @Query(
            """
            select count(o) from ContractOccupantEntity o
            where o.contract.room.id = :roomId
              and o.contract.status in :statuses
              and o.moveInDate <= :asOfDate
              and (o.moveOutDate is null or o.moveOutDate >= :asOfDate)
            """)
    long countCurrentByRoomId(
            @Param("roomId") Long roomId,
            @Param("statuses") Collection<ContractStatus> statuses,
            @Param("asOfDate") LocalDate asOfDate);

    /**
     * Counts occupants still staying on or after {@code fromDate}, regardless of when they moved
     * in, so a back-dated move-in cannot slip past occupants who arrive later.
     */
    @Query(
            """
            select count(o) from ContractOccupantEntity o
            where o.contract.room.id = :roomId
              and o.contract.status in :statuses
              and (o.moveOutDate is null or o.moveOutDate >= :fromDate)
            """)
    long countStayingByRoomIdFrom(
            @Param("roomId") Long roomId,
            @Param("statuses") Collection<ContractStatus> statuses,
            @Param("fromDate") LocalDate fromDate);

    @Query(
            """
            select count(o) from ContractOccupantEntity o
            where o.contract.id = :contractId
              and (o.moveOutDate is null or o.moveOutDate >= :fromDate)
            """)
    long countStayingByContractIdFrom(
            @Param("contractId") Long contractId, @Param("fromDate") LocalDate fromDate);

    @Query(
            """
            select count(o) > 0 from ContractOccupantEntity o
            where o.user.id = :userId
              and o.contract.status in :statuses
              and (o.moveOutDate is null or o.moveOutDate >= :fromDate)
            """)
    boolean existsStayingByUserIdFrom(
            @Param("userId") Long userId,
            @Param("statuses") Collection<ContractStatus> statuses,
            @Param("fromDate") LocalDate fromDate);

    @Query(
            """
            select o from ContractOccupantEntity o
            where o.contract.id = :contractId
              and o.moveInDate <= :asOfDate
              and (o.moveOutDate is null or o.moveOutDate >= :asOfDate)
            order by o.moveInDate asc
            """)
    List<ContractOccupantEntity> findCurrentByContractId(
            @Param("contractId") Long contractId, @Param("asOfDate") LocalDate asOfDate);

    @Query(
            """
            select count(o) from ContractOccupantEntity o
            where o.contract.id = :contractId
              and o.moveInDate <= :asOfDate
              and (o.moveOutDate is null or o.moveOutDate >= :asOfDate)
            """)
    long countCurrentByContractId(
            @Param("contractId") Long contractId, @Param("asOfDate") LocalDate asOfDate);
}
