package com.framehost.websocket;

import java.security.Principal;

import com.framehost.device.DeviceRole;

// Represents an authenticated device in a WebSocket session.
public record AuthenticatedDevicePrincipal(
        String name,
        String roomCode,
        String deviceId,
        DeviceRole role
        ) implements Principal {

    // Returns the authenticated device's principal name.
    @Override
    public String getName() {
        return name;
    }
}
