package com.framehost.room.service;

// Thrown when a room operation conflicts with its business rules.
public class RoomOperationConflictException extends RuntimeException {

    public RoomOperationConflictException(String message) {
        super(message);
    }
}
