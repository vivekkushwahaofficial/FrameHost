package com.framehost.playback.service;

import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.framehost.common.AuthorizationException;
import com.framehost.device.Device;
import com.framehost.device.DeviceRole;
import com.framehost.device.service.DeviceAuthenticationService;
import com.framehost.device.service.DeviceNotFoundException;
import com.framehost.playback.dto.PlaybackPermissionResponse;
import com.framehost.playback.model.PlaybackPermission;
import com.framehost.room.model.Room;
import com.framehost.room.repository.InMemoryRoomRepository;
import com.framehost.room.service.RoomNotFoundException;

// Manages playback permissions for individual Followers.
@Service
public class PlaybackPermissionService {

    private final InMemoryRoomRepository roomRepository;
    private final DeviceAuthenticationService authenticationService;

    // Constructor injection.
    public PlaybackPermissionService(
            InMemoryRoomRepository roomRepository,
            DeviceAuthenticationService authenticationService
    ) {
        this.roomRepository = roomRepository;
        this.authenticationService = authenticationService;
    }

    // Replaces the complete permission set for a Follower.
    public PlaybackPermissionResponse updatePermissions(
            String roomCode,
            String deviceId,
            String authorizationHeader,
            Set<PlaybackPermission> permissions
    ) {

        // Normalize the room code.
        String normalizedRoomCode = roomCode
                .trim()
                .toUpperCase(Locale.ROOT);

        // Find the requested room.
        Room room = roomRepository.findByRoomCode(normalizedRoomCode)
                .orElseThrow(
                        () -> new RoomNotFoundException(normalizedRoomCode)
                );

        // Keep authentication and permission updates atomic
        // with respect to other synchronized room operations.
        synchronized (room) {

            // Authenticate the requesting device.
            Device requestingDevice = authenticationService.authenticate(
                    room,
                    authorizationHeader
            );

            // Only the Host can manage Follower permissions.
            if (requestingDevice.role() != DeviceRole.HOST) {
                throw new AuthorizationException(
                        "Only the Host can manage playback permissions."
                );
            }

            // Find the target device.
            Device targetDevice = room.findDevice(deviceId);

            if (targetDevice == null) {
                throw new DeviceNotFoundException(deviceId);
            }

            // The Host's permissions must never be modified.
            if (targetDevice.role() == DeviceRole.HOST) {
                throw new IllegalArgumentException(
                        "Host permissions cannot be modified."
                );
            }

            // Replace the complete permission set.
            room.updatePlaybackPermissions(
                    deviceId,
                    Set.copyOf(permissions)
            );

            // Return the updated permission state.
            return new PlaybackPermissionResponse(
                    room.roomCode(),
                    deviceId,
                    room.getPlaybackPermissions(deviceId),
                    permissions.isEmpty()
                    ? "All playback permissions revoked."
                    : "Playback permissions updated successfully."
            );
        }
    }
}
