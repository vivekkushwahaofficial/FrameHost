package com.framehost.playback.model;

import java.time.Instant;
import java.util.Objects;

// Immutable snapshot of a room's playback state.
public record PlaybackState(
        VideoProvider videoProvider,
        String videoId,
        PlaybackStatus playbackStatus,
        double currentTime,
        double playbackRate,
        long stateVersion,
        Instant lastUpdatedAt
        ) {

    public PlaybackState {

        Objects.requireNonNull(videoProvider, "Video provider is required.");
        Objects.requireNonNull(videoId, "Video ID is required.");
        Objects.requireNonNull(playbackStatus, "Playback status is required.");
        Objects.requireNonNull(lastUpdatedAt, "Update timestamp is required.");

        if (videoId.isBlank()) {
            throw new IllegalArgumentException("Video ID cannot be blank.");
        }

        if (!Double.isFinite(currentTime) || currentTime < 0) {
            throw new IllegalArgumentException(
                    "Current time must be a finite, non-negative value."
            );
        }

        if (!Double.isFinite(playbackRate) || playbackRate <= 0) {
            throw new IllegalArgumentException(
                    "Playback rate must be a finite, positive value."
            );
        }

        if (stateVersion < 1) {
            throw new IllegalArgumentException(
                    "State version must be greater than zero."
            );
        }
    }

}
