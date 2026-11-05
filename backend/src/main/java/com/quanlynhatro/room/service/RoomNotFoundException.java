package com.quanlynhatro.room.service;

public class RoomNotFoundException extends RuntimeException {

    public RoomNotFoundException(Long id) {
        super("Room not found: " + id);
    }
}
