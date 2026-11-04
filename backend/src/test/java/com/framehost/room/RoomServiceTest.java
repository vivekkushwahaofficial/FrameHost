package com.framehost.room;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.framehost.analytics.service.AnalyticsService;
import com.framehost.common.AuthenticationException;
import com.framehost.common.AuthorizationException;
import com.framehost.device.Device;
import com.framehost.device.DeviceRole;
import com.framehost.device.service.DeviceAuthenticationService;
import com.framehost.device.service.DeviceTokenService;
import com.framehost.room.dto.EndRoomResponse;
import com.framehost.room.model.Room;
import com.framehost.room.model.RoomStatus;
import com.framehost.room.repository.InMemoryRoomRepository;
import com.framehost.room.service.RoomCodeGenerator;
import com.framehost.room.service.RoomNotFoundException;
import com.framehost.room.service.RoomService;

class RoomServiceTest {

    private InMemoryRoomRepository repository;
    private RoomCodeGenerator codeGenerator;
    private DeviceTokenService deviceTokenService;
    private DeviceAuthenticationService authenticationService;
    private AnalyticsService analyticsService;
    private RoomService roomService;

    // Initialize fresh mocks and the service for each test.
    private void initializeTestData() {
        repository = mock(InMemoryRoomRepository.class);
        codeGenerator = mock(RoomCodeGenerator.class);
        deviceTokenService = mock(DeviceTokenService.class);
        authenticationService = mock(DeviceAuthenticationService.class);
        analyticsService = mock(AnalyticsService.class);

        when(deviceTokenService.generateToken())
                .thenReturn("test-host-token");

        when(deviceTokenService.hashToken("test-host-token"))
                .thenReturn("hashed-host-token");

        roomService = new RoomService(
                repository,
                codeGenerator,
                deviceTokenService,
                authenticationService,
                analyticsService
        );
    }

    @Test
    void shouldCreateRoomSuccessfully() {
        initializeTestData();

        when(codeGenerator.generate()).thenReturn("ABC123");
        when(repository.saveIfAbsent(any(Room.class))).thenReturn(true);

        var response = roomService.createRoom();

        assertEquals("ABC123", response.roomCode());
        assertNotNull(response.hostDeviceId());
        assertEquals("test-host-token", response.deviceToken());
        assertNotNull(response.createdAt());
        assertEquals(1, response.deviceCount());
        verify(analyticsService, times(1))
                .recordRoomCreatedSafely();
        verify(repository).saveIfAbsent(any(Room.class));
        verify(deviceTokenService).generateToken();
        verify(deviceTokenService).hashToken("test-host-token");
    }

    @Test
    void shouldRetryWhenRoomCodeAlreadyExists() {
        initializeTestData();

        when(codeGenerator.generate())
                .thenReturn("ABC123")
                .thenReturn("DEF456");

        when(repository.saveIfAbsent(any(Room.class)))
                .thenReturn(false)
                .thenReturn(true);

        var response = roomService.createRoom();

        assertEquals("DEF456", response.roomCode());
        assertEquals("test-host-token", response.deviceToken());
        // Only one successful room creation should produce one event.
        verify(analyticsService, times(1))
                .recordRoomCreatedSafely();
        verify(codeGenerator, times(2)).generate();
        verify(repository, times(2)).saveIfAbsent(any(Room.class));
    }

    @Test
    void shouldFailAfterMaximumGenerationAttempts() {
        initializeTestData();

        when(codeGenerator.generate()).thenReturn("ABC123");
        when(repository.saveIfAbsent(any(Room.class))).thenReturn(false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> roomService.createRoom()
        );

        assertNotNull(exception.getMessage());
        assertFalse(exception.getMessage().isBlank());
        verify(analyticsService, never())
                .recordRoomCreatedSafely();
        verify(codeGenerator, times(5)).generate();
        verify(repository, times(5)).saveIfAbsent(any(Room.class));
    }

    @Test
    void shouldAllowHostToEndActiveRoom() {
        initializeTestData();

        Room room = new Room("ABC123", Instant.now());

        Device host = new Device(
                "host-id",
                "Host Device",
                DeviceRole.HOST,
                Instant.now()
        );

        room.addDevice(host, "hashed-host-token");

        when(repository.findByRoomCode("ABC123"))
                .thenReturn(Optional.of(room));

        when(authenticationService.authenticate(
                room,
                "Bearer test-host-token"
        )).thenReturn(host);

        EndRoomResponse response = roomService.endRoom(
                "ABC123",
                "Bearer test-host-token"
        );

        assertEquals("ABC123", response.roomCode());
        assertEquals(RoomStatus.ENDED, response.status());
        assertNotNull(response.endedAt());
        assertEquals("Room ended successfully.", response.message());

        assertFalse(room.isActive());

        verify(authenticationService).authenticate(
                room,
                "Bearer test-host-token"
        );
    }

    @Test
    void shouldPreventFollowerFromEndingRoom() {
        initializeTestData();

        Room room = new Room("ABC123", Instant.now());

        Device follower = new Device(
                "follower-id",
                "Follower Device",
                DeviceRole.FOLLOWER,
                Instant.now()
        );

        room.addDevice(follower, "hashed-follower-token");

        when(repository.findByRoomCode("ABC123"))
                .thenReturn(Optional.of(room));

        when(authenticationService.authenticate(
                room,
                "Bearer follower-token"
        )).thenReturn(follower);

        AuthorizationException exception = assertThrows(
                AuthorizationException.class,
                () -> roomService.endRoom(
                        "ABC123",
                        "Bearer follower-token"
                )
        );

        assertNotNull(exception);
        assertEquals(RoomStatus.ACTIVE, room.status());
        assertTrue(room.isActive());
    }

    @Test
    void shouldRejectEndingAnAlreadyEndedRoomWhenAuthenticationFails() {
        initializeTestData();

        Room room = createRoomWithHost();

        // End the room, which revokes its stored device tokens.
        room.end();

        when(repository.findByRoomCode("ABC123"))
                .thenReturn(Optional.of(room));

        // The authentication service rejects the revoked Host token.
        when(authenticationService.authenticate(
                room,
                "Bearer test-host-token"
        )).thenThrow(new AuthenticationException());

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> roomService.endRoom(
                        "ABC123",
                        "Bearer test-host-token"
                )
        );

        assertNotNull(exception);

        // Authentication must happen before checking the room status.
        verify(authenticationService).authenticate(
                room,
                "Bearer test-host-token"
        );

        assertEquals(RoomStatus.ENDED, room.status());
        assertFalse(room.isActive());
    }

    @Test
    void shouldRejectEndingRoomWhenAuthenticationIsMissing() {
        initializeTestData();

        Room room = createRoomWithHost();

        when(repository.findByRoomCode("ABC123"))
                .thenReturn(Optional.of(room));

        when(authenticationService.authenticate(room, null))
                .thenThrow(new AuthenticationException());

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> roomService.endRoom("ABC123", null)
        );

        assertNotNull(exception);
        assertTrue(room.isActive());
    }

    @Test
    void shouldRejectEndingRoomWhenTokenIsInvalid() {
        initializeTestData();

        Room room = createRoomWithHost();

        when(repository.findByRoomCode("ABC123"))
                .thenReturn(Optional.of(room));

        when(authenticationService.authenticate(
                room,
                "Bearer invalid-token"
        )).thenThrow(new AuthenticationException());

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> roomService.endRoom(
                        "ABC123",
                        "Bearer invalid-token"
                )
        );

        assertNotNull(exception);
        assertTrue(room.isActive());
    }

    @Test
    void shouldThrowExceptionWhenEndingNonexistentRoom() {
        initializeTestData();

        when(repository.findByRoomCode("UNKNOWN"))
                .thenReturn(Optional.empty());

        RoomNotFoundException exception = assertThrows(
                RoomNotFoundException.class,
                () -> roomService.endRoom(
                        "UNKNOWN",
                        "Bearer test-host-token"
                )
        );

        assertNotNull(exception);
    }

    // Creates an active room containing a Host with a known token hash.
    private Room createRoomWithHost() {
        Room room = new Room("ABC123", Instant.now());

        Device host = new Device(
                "host-id",
                "Host Device",
                DeviceRole.HOST,
                Instant.now()
        );

        room.addDevice(host, "hashed-host-token");

        return room;
    }
}
