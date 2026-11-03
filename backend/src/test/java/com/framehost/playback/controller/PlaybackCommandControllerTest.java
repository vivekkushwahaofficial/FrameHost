package com.framehost.playback.controller;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.framehost.device.Device;
import com.framehost.device.DeviceRole;
import com.framehost.playback.dto.PlaybackCommandMessage;
import com.framehost.playback.dto.PlaybackStateMessage;
import com.framehost.playback.model.PlaybackCommandType;
import com.framehost.playback.model.PlaybackPermission;
import com.framehost.playback.model.PlaybackStatus;
import com.framehost.playback.model.VideoProvider;
import com.framehost.playback.service.PlaybackCommandService;
import com.framehost.room.model.Room;
import com.framehost.room.repository.InMemoryRoomRepository;
import com.framehost.websocket.AuthenticatedDevicePrincipal;

class PlaybackCommandControllerTest {

    private static final String ROOM_CODE = "ABCD12";
    private static final String HOST_ID = "host-123";
    private static final String FOLLOWER_ID = "follower-123";

    private static final String PLAYBACK_TOPIC
            = "/topic/rooms/" + ROOM_CODE + "/playback";

    private final InMemoryRoomRepository roomRepository
            = mock(InMemoryRoomRepository.class);

    private final SimpMessagingTemplate messagingTemplate
            = mock(SimpMessagingTemplate.class);

    private final PlaybackCommandService playbackCommandService
            = new PlaybackCommandService();

    private final PlaybackCommandController controller
            = new PlaybackCommandController(
                    roomRepository,
                    playbackCommandService,
                    messagingTemplate
            );

    private final Room room;

    // JUnit creates a fresh test instance for each test.
    public PlaybackCommandControllerTest() {

        room = new Room(ROOM_CODE, Instant.now());

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

        // Set initial playback state.
        room.updatePlaybackState(
                VideoProvider.YOUTUBE,
                "initialVideo",
                PlaybackStatus.PAUSED,
                10.0,
                1.0
        );

        when(roomRepository.findByRoomCode(ROOM_CODE))
                .thenReturn(Optional.of(room));
    }

    // Creates an authenticated Host identity.
    private AuthenticatedDevicePrincipal hostPrincipal() {

        return new AuthenticatedDevicePrincipal(
                HOST_ID,
                ROOM_CODE,
                HOST_ID,
                DeviceRole.HOST
        );
    }

    // Creates an authenticated Follower identity.
    private AuthenticatedDevicePrincipal followerPrincipal() {

        return new AuthenticatedDevicePrincipal(
                FOLLOWER_ID,
                ROOM_CODE,
                FOLLOWER_ID,
                DeviceRole.FOLLOWER
        );
    }

    // Creates a PLAY command.
    private PlaybackCommandMessage playCommand() {

        return new PlaybackCommandMessage(
                PlaybackCommandType.PLAY,
                null,
                null,
                null,
                null
        );
    }

    // Creates a CHANGE_VIDEO command.
    private PlaybackCommandMessage changeVideoCommand() {

        return new PlaybackCommandMessage(
                PlaybackCommandType.CHANGE_VIDEO,
                VideoProvider.YOUTUBE,
                "newVideo",
                null,
                null
        );
    }

    // Verifies that the expected exception is thrown.
    private <T extends Throwable> void assertThrowsAndVerify(
            Class<T> expectedType,
            Executable executable
    ) {

        T exception = assertThrows(expectedType, executable);

        assertNotNull(exception);
    }

    @Test
    void hostCanChangeVideo() {

        controller.handlePlaybackCommand(
                ROOM_CODE,
                changeVideoCommand(),
                hostPrincipal()
        );

        verify(messagingTemplate).convertAndSend(
                eq(PLAYBACK_TOPIC),
                any(PlaybackStateMessage.class)
        );
    }

    @Test
    void authorizedFollowerCanControlPlayback() {

        room.updatePlaybackPermissions(
                FOLLOWER_ID,
                Set.of(PlaybackPermission.PLAY_PAUSE)
        );

        controller.handlePlaybackCommand(
                ROOM_CODE,
                playCommand(),
                followerPrincipal()
        );

        verify(messagingTemplate).convertAndSend(
                eq(PLAYBACK_TOPIC),
                any(PlaybackStateMessage.class)
        );
    }

    @Test
    void unauthorizedFollowerIsRejected() {

        assertThrowsAndVerify(
                MessagingException.class,
                () -> controller.handlePlaybackCommand(
                        ROOM_CODE,
                        playCommand(),
                        followerPrincipal()
                )
        );

        verify(messagingTemplate, never()).convertAndSend(
                eq(PLAYBACK_TOPIC),
                any(PlaybackStateMessage.class)
        );
    }

    @Test
    void removedDeviceIsRejected() {

        room.removeDevice(FOLLOWER_ID);

        assertThrowsAndVerify(
                MessagingException.class,
                () -> controller.handlePlaybackCommand(
                        ROOM_CODE,
                        playCommand(),
                        followerPrincipal()
                )
        );

        verify(messagingTemplate, never()).convertAndSend(
                eq(PLAYBACK_TOPIC),
                any(PlaybackStateMessage.class)
        );
    }

    @Test
    void outdatedDeviceRoleIsRejected() {

        AuthenticatedDevicePrincipal outdatedPrincipal
                = new AuthenticatedDevicePrincipal(
                        HOST_ID,
                        ROOM_CODE,
                        HOST_ID,
                        DeviceRole.FOLLOWER
                );

        assertThrowsAndVerify(
                MessagingException.class,
                () -> controller.handlePlaybackCommand(
                        ROOM_CODE,
                        playCommand(),
                        outdatedPrincipal
                )
        );

        verify(messagingTemplate, never()).convertAndSend(
                eq(PLAYBACK_TOPIC),
                any(PlaybackStateMessage.class)
        );
    }

    @Test
    void deviceCannotSendCommandToAnotherRoom() {

        assertThrowsAndVerify(
                MessagingException.class,
                () -> controller.handlePlaybackCommand(
                        "OTHER1",
                        playCommand(),
                        hostPrincipal()
                )
        );

        verify(messagingTemplate, never()).convertAndSend(
                any(String.class),
                any(PlaybackStateMessage.class)
        );
    }

    @Test
    void inactiveRoomRejectsCommands() {

        room.end();

        assertThrowsAndVerify(
                MessagingException.class,
                () -> controller.handlePlaybackCommand(
                        ROOM_CODE,
                        playCommand(),
                        hostPrincipal()
                )
        );

        verify(messagingTemplate, never()).convertAndSend(
                eq(PLAYBACK_TOPIC),
                any(PlaybackStateMessage.class)
        );
    }

    @Test
    void unauthenticatedUserIsRejected() {

        assertThrowsAndVerify(
                MessagingException.class,
                () -> controller.handlePlaybackCommand(
                        ROOM_CODE,
                        playCommand(),
                        null
                )
        );

        verify(messagingTemplate, never()).convertAndSend(
                any(String.class),
                any(PlaybackStateMessage.class)
        );
    }

    @Test
    void successfulCommandBroadcastsUpdatedServerState() {

        controller.handlePlaybackCommand(
                ROOM_CODE,
                changeVideoCommand(),
                hostPrincipal()
        );

        verify(messagingTemplate).convertAndSend(
                eq(PLAYBACK_TOPIC),
                any(PlaybackStateMessage.class)
        );
    }

    @Test
    void failedCommandNeverBroadcastsState() {

        PlaybackCommandMessage invalidCommand
                = new PlaybackCommandMessage(
                        PlaybackCommandType.SEEK,
                        null,
                        null,
                        -10.0,
                        null
                );

        assertThrowsAndVerify(
                MessagingException.class,
                () -> controller.handlePlaybackCommand(
                        ROOM_CODE,
                        invalidCommand,
                        hostPrincipal()
                )
        );

        verify(messagingTemplate, never()).convertAndSend(
                eq(PLAYBACK_TOPIC),
                any(PlaybackStateMessage.class)
        );
    }
}
