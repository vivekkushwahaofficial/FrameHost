package com.framehost.device;

import java.time.Instant;

// Represents a device participating in a room.
public record Device(
        String deviceId,
        String deviceName,
        DeviceRole role,
        Instant joinedAt
        ) {

}
