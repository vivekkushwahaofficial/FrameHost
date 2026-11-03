package com.framehost.playback.service;

import org.springframework.stereotype.Service;

import com.framehost.device.Device;
import com.framehost.device.DeviceRole;
import com.framehost.playback.dto.PlaybackCommandMessage;
import com.framehost.playback.model.PlaybackPermission;
import com.framehost.playback.model.PlaybackState;
import com.framehost.playback.model.PlaybackStatus;
import com.framehost.room.model.Room;

// Executes authorized playback commands against a room.
@Service
public class PlaybackCommandService {

    public PlaybackState execute(
            Room room,
            String deviceId,
            PlaybackCommandMessage command
    ) {

        if (room == null) {
            throw new IllegalArgumentException("Room is required.");
        }

        // Keep authorization and state updates atomic.
        synchronized (room) {

            // Reject commands for inactive rooms.
            if (!room.isActive()) {
                throw new IllegalStateException(
                        "Room is unavailable or inactive."
                );
            }

            if (deviceId == null || deviceId.isBlank()) {
                throw new IllegalArgumentException(
                        "Device ID is required."
                );
            }

            if (command == null) {
                throw new IllegalArgumentException(
                        "Playback command is required."
                );
            }

            // Verify that the device still belongs to this room.
            Device device = room.findDevice(deviceId);

            if (device == null) {
                throw new IllegalArgumentException(
                        "Device is no longer a member of this room."
                );
            }

            // Validate the incoming command.
            command.validate();

            // Host has full control. Followers need specific permissions.
            if (device.role() != DeviceRole.HOST) {
                PlaybackPermission requiredPermission
                        = getRequiredPermission(command);

                if (!room.hasPlaybackPermission(
                        deviceId,
                        requiredPermission
                )) {
                    throw new IllegalStateException(
                            "You do not have permission to perform this action."
                    );
                }
            }

            // Read the latest state only after acquiring the room lock.
            PlaybackState current = room.playbackState();

            return switch (command.command()) {

                case PLAY -> {
                    PlaybackState existing
                            = requireCurrentState(current);

                    yield room.updatePlaybackState(
                    existing.videoProvider(),
                    existing.videoId(),
                    PlaybackStatus.PLAYING,
                    resolveTime(command.currentTime(), existing),
                    resolveRate(command.playbackRate(), existing)
                    );
                }

                case PAUSE -> {
                    PlaybackState existing
                            = requireCurrentState(current);

                    yield room.updatePlaybackState(
                    existing.videoProvider(),
                    existing.videoId(),
                    PlaybackStatus.PAUSED,
                    resolveTime(command.currentTime(), existing),
                    resolveRate(command.playbackRate(), existing)
                    );
                }

                case SEEK -> {
                    PlaybackState existing
                            = requireCurrentState(current);

                    yield room.updatePlaybackState(
                    existing.videoProvider(),
                    existing.videoId(),
                    existing.playbackStatus(),
                    command.currentTime(),
                    resolveRate(command.playbackRate(), existing)
                    );
                }

                case CHANGE_VIDEO ->
                    room.updatePlaybackState(
                    command.videoProvider(),
                    command.videoId().trim(),
                    PlaybackStatus.PAUSED,
                    0,
                    resolveRate(command.playbackRate(), current)
                    );
            };
        }
    }

    // Maps each command to its required Follower permission.
    private PlaybackPermission getRequiredPermission(
            PlaybackCommandMessage command
    ) {

        return switch (command.command()) {
            case PLAY, PAUSE ->
                PlaybackPermission.PLAY_PAUSE;
            case SEEK ->
                PlaybackPermission.SEEK;
            case CHANGE_VIDEO ->
                PlaybackPermission.CHANGE_VIDEO;
        };
    }

    private PlaybackState requireCurrentState(PlaybackState state) {

        if (state == null) {
            throw new IllegalStateException(
                    "Select a video before controlling playback."
            );
        }

        return state;
    }

    private double resolveTime(
            Double requestedTime,
            PlaybackState current
    ) {
        return requestedTime != null
                ? requestedTime
                : current.currentTime();
    }

    private double resolveRate(
            Double requestedRate,
            PlaybackState current
    ) {
        if (requestedRate != null) {
            return requestedRate;
        }

        return current != null ? current.playbackRate() : 1.0;
    }
}
