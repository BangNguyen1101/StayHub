package com.quanlynhatro.room.service;

import com.quanlynhatro.room.api.RoomCreateRequest;
import com.quanlynhatro.room.api.RoomOccupantResponse;
import com.quanlynhatro.room.api.RoomResponse;
import com.quanlynhatro.room.api.RoomStatusUpdateRequest;
import com.quanlynhatro.room.api.RoomUpdateRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RoomService {

    Page<RoomResponse> search(String keyword, String status, Pageable pageable);

    Page<RoomResponse> available(Pageable pageable);

    RoomResponse getById(Long id);

    List<RoomOccupantResponse> currentOccupants(Long id);

    RoomResponse create(RoomCreateRequest request);

    RoomResponse update(Long id, RoomUpdateRequest request);

    RoomResponse updateStatus(Long id, RoomStatusUpdateRequest request);
}
