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

import com.framehost.room.model.Room;
import com.framehost.room.repository.InMemoryRoomRepository;
import com.framehost.room.service.RoomCodeGenerator;
import com.framehost.room.service.RoomService;

class RoomServiceTest {

    private InMemoryRoomRepository repository;
    private RoomCodeGenerator codeGenerator;
    private RoomService roomService;

    // Initialize fresh mocks and the service for each test.
    private void initializeTestData() {
        repository = mock(InMemoryRoomRepository.class);
        codeGenerator = mock(RoomCodeGenerator.class);

        roomService = new RoomService(repository, codeGenerator);
    }

    @Test
    void shouldCreateRoomSuccessfully() {
        initializeTestData();

        when(codeGenerator.generate()).thenReturn("ABC123");
        when(repository.saveIfAbsent(any(Room.class))).thenReturn(true);

        var response = roomService.createRoom();

        assertEquals("ABC123", response.roomCode());
        assertNotNull(response.createdAt());

        verify(repository).saveIfAbsent(any(Room.class));
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

        verify(codeGenerator, times(5)).generate();
        verify(repository, times(5)).saveIfAbsent(any(Room.class));
    }
}
