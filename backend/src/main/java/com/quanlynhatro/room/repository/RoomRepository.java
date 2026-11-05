package com.quanlynhatro.room.repository;

import com.quanlynhatro.room.domain.RoomEntity;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<RoomEntity, Long> {

    boolean existsByRoomCodeIgnoreCase(String roomCode);

    Optional<RoomEntity> findByRoomCodeIgnoreCase(String roomCode);

    boolean existsByRoomCodeIgnoreCaseAndIdNot(String roomCode, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RoomEntity r where r.id = :id")
    Optional<RoomEntity> findByIdForUpdate(@Param("id") Long id);

    Page<RoomEntity> findByStatus(String status, Pageable pageable);

    @Query(
            """
            select r from RoomEntity r
            where (:keyword is null or :keyword = ''
                or lower(r.roomCode) like lower(concat('%', :keyword, '%'))
                or lower(r.roomName) like lower(concat('%', :keyword, '%')))
              and (:status is null or :status = '' or r.status = :status)
            """)
    Page<RoomEntity> search(
            @Param("keyword") String keyword, @Param("status") String status, Pageable pageable);
}
