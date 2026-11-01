package com.framehost.room;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.framehost.device.Device;
import com.framehost.device.DeviceRole;
import com.framehost.room.dto.JoinRoomRequest;
import com.framehost.room.model.Room;
import com.framehost.room.repository.InMemoryRoomRepository;
import com.framehost.room.service.RoomCodeGenerator;
import com.framehost.room.service.RoomFullException;
import com.framehost.room.service.RoomNotFoundException;
import com.framehost.room.service.RoomService;

class RoomJoinServiceTest {

    private InMemoryRoomRepository repository;
    private RoomService roomService;
    private Room room;

    // Prepare fresh test dependencies and a room for each test.
    private void initializeTestData() {
        repository = mock(InMemoryRoomRepository.class);
        RoomCodeGenerator codeGenerator = mock(RoomCodeGenerator.class);

        roomService = new RoomService(repository, codeGenerator);
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

        when(repository.findByRoomCode("ABC123"))
                .thenReturn(Optional.of(room));

        var response = roomService.joinRoom(
                "ABC123",
                new JoinRoomRequest("Laptop")
        );

        assertEquals("ABC123", response.roomCode());
        assertEquals("Laptop", response.deviceName());
        assertEquals(DeviceRole.FOLLOWER, response.role());
        assertEquals(2, response.deviceCount());
        assertEquals(10, response.maxDevices());
        assertNotNull(response.deviceId());
    }

    @Test
    void shouldThrowExceptionWhenRoomDoesNotExist() {
        initializeTestData();

        when(repository.findByRoomCode("UNKNOWN"))
                .thenReturn(Optional.empty());

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

        when(repository.findByRoomCode("ABC123"))
                .thenReturn(Optional.of(room));

        assertEquals(10, room.deviceCount());

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
