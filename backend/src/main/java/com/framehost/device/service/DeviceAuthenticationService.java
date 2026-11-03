package com.framehost.device.service;

import org.springframework.stereotype.Service;

import com.framehost.common.AuthenticationException;
import com.framehost.device.Device;
import com.framehost.room.model.Room;

// Authenticates devices using their Bearer tokens.
@Service
public class DeviceAuthenticationService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final DeviceTokenService deviceTokenService;

    public DeviceAuthenticationService(
            DeviceTokenService deviceTokenService
    ) {
        this.deviceTokenService = deviceTokenService;
    }

    // Identifies a device from its Authorization header.
    public Device authenticate(
            Room room,
            String authorizationHeader
    ) {

        // Reject missing or incorrectly formatted credentials.
        if (room == null
                || authorizationHeader == null
                || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new AuthenticationException();
        }

        // Extract the original token.
        String token = authorizationHeader
                .substring(BEARER_PREFIX.length())
                .trim();

        if (token.isEmpty()) {
            throw new AuthenticationException();
        }

        // Hash the supplied token before checking stored credentials.
        String tokenHash = deviceTokenService.hashToken(token);

        // Find the device associated with this token.
        Device device = room.findDeviceByTokenHash(tokenHash);

        if (device == null) {
            throw new AuthenticationException();
        }

        return device;
    }
}
