package com.quanlynhatro.room.service;

public class RoomStatusChangeNotAllowedException extends RuntimeException {

    public RoomStatusChangeNotAllowedException(String currentStatus, String requestedStatus) {
        super("Room status cannot change from " + currentStatus + " to " + requestedStatus);
    }
}
