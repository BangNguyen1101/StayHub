package com.quanlynhatro.room.api;

import com.quanlynhatro.room.service.RoomService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/api/admin/rooms")
    public PageResponse<RoomResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return PageResponse.from(roomService.search(keyword, status, pageable));
    }

    @GetMapping("/api/admin/rooms/{id}")
    public RoomResponse getById(@PathVariable @Positive Long id) {
        return roomService.getById(id);
    }

    @GetMapping("/api/admin/rooms/{id}/occupants")
    public List<RoomOccupantResponse> currentOccupants(@PathVariable @Positive Long id) {
        return roomService.currentOccupants(id);
    }

    @PostMapping("/api/admin/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse create(@Valid @RequestBody RoomCreateRequest request) {
        return roomService.create(request);
    }

    @PutMapping("/api/admin/rooms/{id}")
    public RoomResponse update(
            @PathVariable @Positive Long id, @Valid @RequestBody RoomUpdateRequest request) {
        return roomService.update(id, request);
    }

    @PatchMapping("/api/admin/rooms/{id}/status")
    public RoomResponse updateStatus(
            @PathVariable @Positive Long id, @Valid @RequestBody RoomStatusUpdateRequest request) {
        return roomService.updateStatus(id, request);
    }

    @GetMapping("/api/rooms/available")
    public PageResponse<RoomResponse> available(Pageable pageable) {
        return PageResponse.from(roomService.available(pageable));
    }
}
