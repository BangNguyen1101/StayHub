package com.quanlynhatro.utility.service;

public class RoomForUtilityNotFoundException extends UtilityBusinessException {
    public RoomForUtilityNotFoundException(Long id) {
        super("Room not found: " + id);
    }
}
