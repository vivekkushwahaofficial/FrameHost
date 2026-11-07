package com.framehost.room.service;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.framehost.playback.model.PlaybackState;
import com.framehost.playback.model.PlaybackStatus;
import com.framehost.playback.model.VideoProvider;
import com.framehost.room.model.Room;

class RoomPlaybackStateTest {

    private Room room;

    @BeforeEach
    public void setUp() {
        room = new Room("ABCD12", Instant.now());
    }

    @Test
    void shouldInitiallyHaveNoPlaybackState() {
        assertNull(room.playbackState());
    }

    @Test
    void shouldCreateInitialPlaybackState() {

        PlaybackState state = room.updatePlaybackState(
                VideoProvider.YOUTUBE,
                "dQw4w9WgXcQ",
                PlaybackStatus.PAUSED,
                0,
                1.0
        );

        assertNotNull(state);
        assertEquals("dQw4w9WgXcQ", state.videoId());
        assertEquals(VideoProvider.YOUTUBE, state.videoProvider());
        assertEquals(PlaybackStatus.PAUSED, state.playbackStatus());
        assertEquals(0, state.currentTime());
        assertEquals(1.0, state.playbackRate());
        assertEquals(1, state.stateVersion());
        assertNotNull(state.lastUpdatedAt());
    }

    @Test
    void shouldIncrementVersionWhenPlaybackStateChanges() {

        room.updatePlaybackState(
                VideoProvider.YOUTUBE,
                "dQw4w9WgXcQ",
                PlaybackStatus.PAUSED,
                0,
                1.0
        );

        PlaybackState updatedState = room.updatePlaybackState(
                VideoProvider.YOUTUBE,
                "dQw4w9WgXcQ",
                PlaybackStatus.PLAYING,
                10,
                1.0
        );

        assertEquals(2, updatedState.stateVersion());
        assertEquals(PlaybackStatus.PLAYING, updatedState.playbackStatus());
        assertEquals(10, updatedState.currentTime());
    }

    @Test
    void shouldRejectPlaybackUpdateForEndedRoom() {

        room.end();

        assertNotNull(
                assertThrows(
                        IllegalStateException.class,
                        () -> room.updatePlaybackState(
                                VideoProvider.YOUTUBE,
                                "dQw4w9WgXcQ",
                                PlaybackStatus.PLAYING,
                                0,
                                1.0
                        )
                )
        );
    }

    @Test
    void shouldClearPlaybackStateWhenRoomEnds() {

        room.updatePlaybackState(
                VideoProvider.YOUTUBE,
                "dQw4w9WgXcQ",
                PlaybackStatus.PLAYING,
                10,
                1.0
        );

        room.end();

        assertNull(room.playbackState());
    }

    @Test
    void shouldRejectInvalidPlaybackPosition() {

        assertNotNull(
                assertThrows(
                        IllegalArgumentException.class,
                        () -> room.updatePlaybackState(
                                VideoProvider.YOUTUBE,
                                "dQw4w9WgXcQ",
                                PlaybackStatus.PLAYING,
                                -5,
                                1.0
                        )
                )
        );
    }

    @Test
    void shouldRejectInvalidPlaybackRate() {

        assertNotNull(
                assertThrows(
                        IllegalArgumentException.class,
                        () -> room.updatePlaybackState(
                                VideoProvider.YOUTUBE,
                                "dQw4w9WgXcQ",
                                PlaybackStatus.PLAYING,
                                0,
                                0
                        )
                )
        );
    }
}
