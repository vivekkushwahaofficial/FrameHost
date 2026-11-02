package com.framehost.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.framehost.device.service.DeviceAuthenticationService;
import com.framehost.device.service.DeviceTokenService;
import com.framehost.room.model.Room;
import com.framehost.room.repository.InMemoryRoomRepository;
import com.framehost.room.service.RoomCodeGenerator;
import com.framehost.room.service.RoomService;

class RoomServiceTest {

    private InMemoryRoomRepository repository;
    private RoomCodeGenerator codeGenerator;
    private DeviceTokenService deviceTokenService;
    private DeviceAuthenticationService authenticationService;
    private RoomService roomService;

    // Initialize fresh mocks and the service for each test.
    private void initializeTestData() {
        repository = mock(InMemoryRoomRepository.class);
        codeGenerator = mock(RoomCodeGenerator.class);
        deviceTokenService = mock(DeviceTokenService.class);
        authenticationService = mock(DeviceAuthenticationService.class);

        // Configure a predictable token for testing.
        when(deviceTokenService.generateToken())
                .thenReturn("test-host-token");

        when(deviceTokenService.hashToken("test-host-token"))
                .thenReturn("hashed-host-token");

        // Create the service with all required dependencies.
        roomService = new RoomService(
                repository,
                codeGenerator,
                deviceTokenService,
                authenticationService
        );
    }

    @Test
    void shouldCreateRoomSuccessfully() {
        initializeTestData();

        // Configure room creation dependencies.
        when(codeGenerator.generate()).thenReturn("ABC123");
        when(repository.saveIfAbsent(any(Room.class))).thenReturn(true);

        // Execute room creation.
        var response = roomService.createRoom();

        // Verify the returned room information.
        assertEquals("ABC123", response.roomCode());
        assertNotNull(response.hostDeviceId());
        assertEquals("test-host-token", response.deviceToken());
        assertNotNull(response.createdAt());
        assertEquals(1, response.deviceCount());

        // Verify repository and token service interactions.
        verify(repository).saveIfAbsent(any(Room.class));
        verify(deviceTokenService).generateToken();
        verify(deviceTokenService).hashToken("test-host-token");
    }

    @Test
    void shouldRetryWhenRoomCodeAlreadyExists() {
        initializeTestData();

        // Return a duplicate code first, followed by a unique code.
        when(codeGenerator.generate())
                .thenReturn("ABC123")
                .thenReturn("DEF456");

        // First save fails; second save succeeds.
        when(repository.saveIfAbsent(any(Room.class)))
                .thenReturn(false)
                .thenReturn(true);

        // Execute room creation.
        var response = roomService.createRoom();

        // Verify the successful attempt.
        assertEquals("DEF456", response.roomCode());
        assertEquals("test-host-token", response.deviceToken());

        verify(codeGenerator, times(2)).generate();
        verify(repository, times(2)).saveIfAbsent(any(Room.class));
    }

    @Test
    void shouldFailAfterMaximumGenerationAttempts() {
        initializeTestData();

        // Always return the same room code.
        when(codeGenerator.generate()).thenReturn("ABC123");

        // Simulate room code collisions.
        when(repository.saveIfAbsent(any(Room.class))).thenReturn(false);

        // Verify that creation eventually fails.
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> roomService.createRoom()
        );

        assertNotNull(exception.getMessage());
        assertFalse(exception.getMessage().isBlank());

        verify(codeGenerator, times(5)).generate();
        verify(repository, times(5)).saveIfAbsent(any(Room.class));
    }
}
