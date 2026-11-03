package com.framehost.device.dto;

import java.time.Instant;
import java.util.Set;

import com.framehost.device.DeviceRole;
import com.framehost.playback.model.PlaybackPermission;

// Public device information returned to the Host.
public record DeviceResponse(
        String deviceId,
        String deviceName,
        DeviceRole role,
        Instant joinedAt,
        Set<PlaybackPermission> permissions
        ) {

}
