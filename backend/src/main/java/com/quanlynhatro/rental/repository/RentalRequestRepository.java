package com.quanlynhatro.rental.repository;

import com.quanlynhatro.rental.api.RentalRequestStatus;
import com.quanlynhatro.rental.domain.RentalRequestEntity;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface RentalRequestRepository extends JpaRepository<RentalRequestEntity, Long> {

    boolean existsByApplicantUser_IdAndRoom_IdAndStatusIn(
            Long applicantUserId, Long roomId, Collection<RentalRequestStatus> statuses);

    Optional<RentalRequestEntity> findByRequestCode(String requestCode);

    boolean existsByRequestCode(String requestCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RentalRequestEntity r where r.id = :id")
    Optional<RentalRequestEntity> findByIdForUpdate(@Param("id") Long id);

    @Query(
            """
            select r from RentalRequestEntity r
            where (:keyword is null or :keyword = ''
                or lower(r.requestCode) like lower(concat('%', :keyword, '%'))
                or lower(r.contactName) like lower(concat('%', :keyword, '%'))
                or lower(r.contactPhone) like lower(concat('%', :keyword, '%'))
                or lower(r.room.roomCode) like lower(concat('%', :keyword, '%')))
              and (:status is null or r.status = :status)
              and (:applicantUserId is null or r.applicantUser.id = :applicantUserId)
              and (:roomId is null or r.room.id = :roomId)
            """)
    Page<RentalRequestEntity> search(
            @Param("keyword") String keyword,
            @Param("status") RentalRequestStatus status,
            @Param("applicantUserId") Long applicantUserId,
            @Param("roomId") Long roomId,
            Pageable pageable);
}
