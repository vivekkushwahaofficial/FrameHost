package com.framehost.room.dto;

import java.time.Instant;

import com.framehost.room.model.RoomStatus;

// Represents the result of ending a room.
public record EndRoomResponse(
        String roomCode,
        RoomStatus status,
        Instant endedAt,
        String message
        ) {

}
