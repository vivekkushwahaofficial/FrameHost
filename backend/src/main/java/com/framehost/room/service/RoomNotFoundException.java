package com.framehost.room.service;

// Exception thrown when a requested room does not exist.
public class RoomNotFoundException extends RuntimeException {

    public RoomNotFoundException(String roomCode) {
        super("Room not found: " + roomCode);
    }
}
