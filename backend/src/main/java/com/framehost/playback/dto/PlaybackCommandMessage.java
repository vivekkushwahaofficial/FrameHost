package com.framehost.playback.dto;

import java.util.Objects;

import com.framehost.playback.model.PlaybackCommandType;
import com.framehost.playback.model.VideoProvider;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Incoming playback command sent by the Host.
public record PlaybackCommandMessage(
        @NotNull
        PlaybackCommandType command,
        VideoProvider videoProvider,
        String videoId,
        Double currentTime,
        @Positive
        Double playbackRate
        ) {

    // Validates fields required by each command.
    public void validate() {

        Objects.requireNonNull(command, "Playback command is required.");

        switch (command) {

            case PLAY, PAUSE -> {
                validateOptionalPositionAndRate();
            }

            case SEEK -> {
                if (currentTime == null
                        || !Double.isFinite(currentTime)
                        || currentTime < 0) {
                    throw new IllegalArgumentException(
                            "SEEK requires a finite, non-negative currentTime."
                    );
                }

                validateOptionalRate();
            }

            case CHANGE_VIDEO -> {
                if (videoProvider == null) {
                    throw new IllegalArgumentException(
                            "CHANGE_VIDEO requires a video provider."
                    );
                }

                if (videoId == null || videoId.isBlank()) {
                    throw new IllegalArgumentException(
                            "CHANGE_VIDEO requires a video ID."
                    );
                }

                validateOptionalRate();
            }
        }
    }

    private void validateOptionalPositionAndRate() {

        if (currentTime != null
                && (!Double.isFinite(currentTime) || currentTime < 0)) {
            throw new IllegalArgumentException(
                    "Current time must be finite and non-negative."
            );
        }

        validateOptionalRate();
    }

    private void validateOptionalRate() {

        if (playbackRate != null
                && (!Double.isFinite(playbackRate) || playbackRate <= 0)) {
            throw new IllegalArgumentException(
                    "Playback rate must be finite and positive."
            );
        }
    }
}
