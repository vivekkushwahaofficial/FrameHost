package com.framehost.room.service;

// Exception thrown when a room reaches its capacity.
public class RoomFullException extends RuntimeException {

    public RoomFullException(String roomCode) {
        super("Room is full: " + roomCode);
    }
}
