package com.framehost.room;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.framehost.device.Device;
import com.framehost.device.DeviceRole;
import com.framehost.device.service.DeviceAuthenticationService;
import com.framehost.device.service.DeviceTokenService;
import com.framehost.room.dto.JoinRoomRequest;
import com.framehost.room.model.Room;
import com.framehost.room.repository.InMemoryRoomRepository;
import com.framehost.room.service.RoomCodeGenerator;
import com.framehost.room.service.RoomFullException;
import com.framehost.room.service.RoomNotFoundException;
import com.framehost.room.service.RoomService;

class RoomJoinServiceTest {

    private InMemoryRoomRepository repository;
    private DeviceTokenService deviceTokenService;
    private DeviceAuthenticationService authenticationService;
    private RoomService roomService;
    private Room room;

    // Prepare fresh test dependencies and a room for each test.
    private void initializeTestData() {
        repository = mock(InMemoryRoomRepository.class);

        RoomCodeGenerator codeGenerator = mock(RoomCodeGenerator.class);
        deviceTokenService = mock(DeviceTokenService.class);
        authenticationService = mock(DeviceAuthenticationService.class);

        // Configure predictable authentication tokens.
        when(deviceTokenService.generateToken())
                .thenReturn("test-follower-token");

        when(deviceTokenService.hashToken("test-follower-token"))
                .thenReturn("hashed-follower-token");

        // Initialize the service with all dependencies.
        roomService = new RoomService(
                repository,
                codeGenerator,
                deviceTokenService,
                authenticationService
        );

        // Create a room with an existing Host.
        room = new Room("ABC123", Instant.now());

        Device host = new Device(
                UUID.randomUUID().toString(),
                "Host",
                DeviceRole.HOST,
                Instant.now()
        );

        room.addDevice(host);
    }

    @Test
    void shouldJoinExistingRoomAsFollower() {
        initializeTestData();

        // Return the existing room.
        when(repository.findByRoomCode("ABC123"))
                .thenReturn(Optional.of(room));

        // Join the room.
        var response = roomService.joinRoom(
                "ABC123",
                new JoinRoomRequest("Laptop")
        );

        // Verify the joining device information.
        assertEquals("ABC123", response.roomCode());
        assertEquals("Laptop", response.deviceName());
        assertEquals("test-follower-token", response.deviceToken());
        assertEquals(DeviceRole.FOLLOWER, response.role());
        assertEquals(2, response.deviceCount());
        assertEquals(10, response.maxDevices());
        assertNotNull(response.deviceId());
        assertNotNull(response.joinedAt());

        // Verify token generation and hashing.
        verify(deviceTokenService).generateToken();
        verify(deviceTokenService).hashToken("test-follower-token");
    }

    @Test
    void shouldThrowExceptionWhenRoomDoesNotExist() {
        initializeTestData();

        // Simulate an unknown room.
        when(repository.findByRoomCode("UNKNOWN"))
                .thenReturn(Optional.empty());

        // Verify the expected exception.
        RoomNotFoundException exception = assertThrows(
                RoomNotFoundException.class,
                () -> roomService.joinRoom(
                        "UNKNOWN",
                        new JoinRoomRequest("Laptop")
                )
        );

        assertEquals("Room not found: UNKNOWN", exception.getMessage());
    }

    @Test
    void shouldRejectDeviceWhenRoomIsFull() {
        initializeTestData();

        // Add nine followers to the existing Host.
        for (int i = 1; i < Room.MAX_DEVICES; i++) {
            room.addDevice(new Device(
                    UUID.randomUUID().toString(),
                    "Device " + i,
                    DeviceRole.FOLLOWER,
                    Instant.now()
            ));
        }

        // Return the full room.
        when(repository.findByRoomCode("ABC123"))
                .thenReturn(Optional.of(room));

        assertEquals(10, room.deviceCount());

        // Verify that another device cannot join.
        RoomFullException exception = assertThrows(
                RoomFullException.class,
                () -> roomService.joinRoom(
                        "ABC123",
                        new JoinRoomRequest("Extra device")
                )
        );

        assertEquals("Room is full: ABC123", exception.getMessage());
        assertEquals(10, room.deviceCount());
    }
}
