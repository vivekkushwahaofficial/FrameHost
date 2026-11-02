
package com.framehost.device.dto;

import java.time.Instant;

import com.framehost.device.DeviceRole;

// Public device information returned to the Host.
public record DeviceResponse(
        String deviceId,
        String deviceName,
        DeviceRole role,
        Instant joinedAt
) {
}
