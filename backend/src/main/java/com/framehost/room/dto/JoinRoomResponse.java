package com.framehost.room.dto;

import java.time.Instant;

import com.framehost.device.DeviceRole;

// Response returned after a device joins a room.
public record JoinRoomResponse(
        String roomCode,
        String deviceId,
        String deviceName,
        DeviceRole role,
        Instant joinedAt,
        int deviceCount,
        int maxDevices
        ) {

}
