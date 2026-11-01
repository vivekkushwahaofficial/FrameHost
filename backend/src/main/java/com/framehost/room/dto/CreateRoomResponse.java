package com.framehost.room.dto;

import java.time.Instant;

import com.framehost.device.DeviceRole;

// Response returned when a room is created.
public record CreateRoomResponse(
        String roomCode,
        String hostDeviceId,
        DeviceRole role,
        Instant createdAt,
        int deviceCount,
        int maxDevices
        ) {

}
