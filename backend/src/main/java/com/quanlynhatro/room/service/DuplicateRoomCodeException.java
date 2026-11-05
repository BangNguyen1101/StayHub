package com.quanlynhatro.room.service;

public class DuplicateRoomCodeException extends RuntimeException {

    public DuplicateRoomCodeException(String roomCode) {
        super("Room code already exists: " + roomCode);
    }
}
