package com.framehost.playback.service;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.framehost.device.Device;
import com.framehost.device.DeviceRole;
import com.framehost.playback.dto.PlaybackCommandMessage;
import com.framehost.playback.model.PlaybackCommandType;
import com.framehost.playback.model.PlaybackPermission;
import com.framehost.playback.model.PlaybackState;
import com.framehost.playback.model.PlaybackStatus;
import com.framehost.playback.model.VideoProvider;
import com.framehost.room.model.Room;

class PlaybackCommandServiceTest {

    private static final String HOST_ID = "host-123";
    private static final String FOLLOWER_ID = "follower-123";

    private final PlaybackCommandService service;
    private final Room room;

    // JUnit creates a fresh test instance for each test.
    // The constructor initializes the required test data.
    public PlaybackCommandServiceTest() {

        service = new PlaybackCommandService();

        room = new Room("ABCD12", Instant.now());

        // Register the Host.
        room.addDevice(new Device(
                HOST_ID,
                "Host Device",
                DeviceRole.HOST,
                Instant.now()
        ));

        // Register a Follower.
        room.addDevice(new Device(
                FOLLOWER_ID,
                "Follower Device",
                DeviceRole.FOLLOWER,
                Instant.now()
        ));

        // Set an initial video.
        room.updatePlaybackState(
                VideoProvider.YOUTUBE,
                "initialVideo",
                PlaybackStatus.PAUSED,
                10.0,
                1.0
        );
    }

    // Checks that an expected exception is thrown.
    // Also verifies that JUnit returns a valid exception.
    private <T extends Throwable> void assertThrowsAndVerify(
            Class<T> expectedType,
            Executable executable
    ) {

        T exception = assertThrows(expectedType, executable);

        assertNotNull(exception);
    }

    @Test
    void hostShouldHaveFullPlaybackControl() {

        PlaybackCommandMessage command = new PlaybackCommandMessage(
                PlaybackCommandType.CHANGE_VIDEO,
                VideoProvider.YOUTUBE,
                "newVideo",
                null,
                null
        );

        PlaybackState result = service.execute(
                room,
                HOST_ID,
                command
        );

        assertNotNull(result);
        assertEquals("newVideo", result.videoId());
        assertEquals(PlaybackStatus.PAUSED, result.playbackStatus());
    }

    @Test
    void followerShouldNotHavePlaybackControlByDefault() {

        PlaybackCommandMessage command = new PlaybackCommandMessage(
                PlaybackCommandType.PLAY,
                null,
                null,
                null,
                null
        );

        assertThrowsAndVerify(
                IllegalStateException.class,
                () -> service.execute(room, FOLLOWER_ID, command)
        );
    }

    @Test
    void followerWithPlayPausePermissionShouldPlay() {

        room.updatePlaybackPermissions(
                FOLLOWER_ID,
                Set.of(PlaybackPermission.PLAY_PAUSE)
        );

        PlaybackCommandMessage command = new PlaybackCommandMessage(
                PlaybackCommandType.PLAY,
                null,
                null,
                null,
                null
        );

        PlaybackState result = service.execute(
                room,
                FOLLOWER_ID,
                command
        );

        assertEquals(PlaybackStatus.PLAYING, result.playbackStatus());
    }

    @Test
    void followerWithPlayPausePermissionShouldPause() {

        room.updatePlaybackPermissions(
                FOLLOWER_ID,
                Set.of(PlaybackPermission.PLAY_PAUSE)
        );

        PlaybackCommandMessage command = new PlaybackCommandMessage(
                PlaybackCommandType.PAUSE,
                null,
                null,
                null,
                null
        );

        PlaybackState result = service.execute(
                room,
                FOLLOWER_ID,
                command
        );

        assertEquals(PlaybackStatus.PAUSED, result.playbackStatus());
    }

    @Test
    void followerWithSeekPermissionShouldSeek() {

        room.updatePlaybackPermissions(
                FOLLOWER_ID,
                Set.of(PlaybackPermission.SEEK)
        );

        PlaybackCommandMessage command = new PlaybackCommandMessage(
                PlaybackCommandType.SEEK,
                null,
                null,
                45.0,
                null
        );

        PlaybackState result = service.execute(
                room,
                FOLLOWER_ID,
                command
        );

        assertEquals(45.0, result.currentTime());
    }

    @Test
    void followerWithChangeVideoPermissionShouldChangeVideo() {

        room.updatePlaybackPermissions(
                FOLLOWER_ID,
                Set.of(PlaybackPermission.CHANGE_VIDEO)
        );

        PlaybackCommandMessage command = new PlaybackCommandMessage(
                PlaybackCommandType.CHANGE_VIDEO,
                VideoProvider.YOUTUBE,
                "followerSelectedVideo",
                null,
                null
        );

        PlaybackState result = service.execute(
                room,
                FOLLOWER_ID,
                command
        );

        assertEquals(
                "followerSelectedVideo",
                result.videoId()
        );
    }

    @Test
    void followerShouldBeRejectedAfterPermissionRevocation() {

        // First grant permission.
        room.updatePlaybackPermissions(
                FOLLOWER_ID,
                Set.of(PlaybackPermission.PLAY_PAUSE)
        );

        // Then revoke all permissions.
        room.updatePlaybackPermissions(
                FOLLOWER_ID,
                Set.of()
        );

        PlaybackCommandMessage command = new PlaybackCommandMessage(
                PlaybackCommandType.PLAY,
                null,
                null,
                null,
                null
        );

        assertThrowsAndVerify(
                IllegalStateException.class,
                () -> service.execute(room, FOLLOWER_ID, command)
        );
    }

    @Test
    void removedFollowerShouldNotExecuteCommands() {

        room.updatePlaybackPermissions(
                FOLLOWER_ID,
                Set.of(PlaybackPermission.PLAY_PAUSE)
        );

        room.removeDevice(FOLLOWER_ID);

        PlaybackCommandMessage command = new PlaybackCommandMessage(
                PlaybackCommandType.PLAY,
                null,
                null,
                null,
                null
        );

        assertThrowsAndVerify(
                IllegalArgumentException.class,
                () -> service.execute(room, FOLLOWER_ID, command)
        );
    }

    @Test
    void inactiveRoomShouldRejectPlaybackCommands() {

        room.end();

        PlaybackCommandMessage command = new PlaybackCommandMessage(
                PlaybackCommandType.PLAY,
                null,
                null,
                null,
                null
        );

        assertThrowsAndVerify(
                IllegalStateException.class,
                () -> service.execute(room, HOST_ID, command)
        );
    }

    @Test
    void playShouldRequireAnExistingVideo() {

        Room emptyRoom = new Room("EMPTY1", Instant.now());

        emptyRoom.addDevice(new Device(
                HOST_ID,
                "Host Device",
                DeviceRole.HOST,
                Instant.now()
        ));

        PlaybackCommandMessage command = new PlaybackCommandMessage(
                PlaybackCommandType.PLAY,
                null,
                null,
                null,
                null
        );

        assertThrowsAndVerify(
                IllegalStateException.class,
                () -> service.execute(emptyRoom, HOST_ID, command)
        );
    }

    @Test
    void seekShouldRejectNegativePosition() {

        PlaybackCommandMessage command = new PlaybackCommandMessage(
                PlaybackCommandType.SEEK,
                null,
                null,
                -10.0,
                null
        );

        assertThrowsAndVerify(
                IllegalArgumentException.class,
                () -> service.execute(room, HOST_ID, command)
        );
    }

    @Test
    void hostCanChangeToLocalMediaSession() {

        // Create a local media session identifier.
        String mediaSessionId = "8b7f2c91-5d4a-4e63-9a12-123456789abc";

        // Create a command to select local media.
        PlaybackCommandMessage command = new PlaybackCommandMessage(
                PlaybackCommandType.CHANGE_VIDEO,
                VideoProvider.LOCAL,
                mediaSessionId,
                null,
                null
        );

        // Execute the command as the Host.
        PlaybackState result = service.execute(
                room,
                HOST_ID,
                command
        );

        // Verify the resulting playback state.
        assertNotNull(result);
        assertEquals(VideoProvider.LOCAL, result.videoProvider());
        assertEquals(mediaSessionId, result.videoId());
        assertEquals(PlaybackStatus.PAUSED, result.playbackStatus());
    }
}
