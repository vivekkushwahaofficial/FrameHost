package com.framehost.room.model;

// Defines the possible lifecycle states of a room.
public enum RoomStatus {

    // The room is available for normal operations.
    ACTIVE,
    // The Host has explicitly ended the room.
    ENDED,
    // The room has reached its expiration time.
    EXPIRED
}
