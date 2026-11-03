package com.framehost.playback.dto;

import java.time.Instant;

import com.framehost.playback.model.PlaybackState;
import com.framehost.playback.model.PlaybackStatus;
import com.framehost.playback.model.VideoProvider;

// WebSocket message containing the current playback state.
public record PlaybackStateMessage(
        VideoProvider videoProvider,
        String videoId,
        PlaybackStatus playbackStatus,
        double currentTime,
        double playbackRate,
        long stateVersion,
        Instant lastUpdatedAt
        ) {

    // Converts the domain model into a WebSocket DTO.
    public static PlaybackStateMessage from(PlaybackState state) {

        return new PlaybackStateMessage(
                state.videoProvider(),
                state.videoId(),
                state.playbackStatus(),
                state.currentTime(),
                state.playbackRate(),
                state.stateVersion(),
                state.lastUpdatedAt()
        );
    }

}
