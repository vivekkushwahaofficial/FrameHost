package com.framehost.room.repository;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.framehost.room.model.Room;

class InMemoryRoomRepositoryTest {

    private InMemoryRoomRepository repository;

    @BeforeEach
    public void setUp() {
        repository = new InMemoryRoomRepository();
    }

    @Test
    void countActiveRoomsShouldReturnZeroWhenRepositoryIsEmpty() {
        assertEquals(0, repository.countActiveRooms());
    }

    @Test
    void countActiveRoomsShouldCountOnlyActiveRooms() {
        // Arrange: Create three rooms.
        Room room1 = new Room("ROOM001", Instant.now());
        Room room2 = new Room("ROOM002", Instant.now());
        Room room3 = new Room("ROOM003", Instant.now());

        // Save all three rooms.
        repository.saveIfAbsent(room1);
        repository.saveIfAbsent(room2);
        repository.saveIfAbsent(room3);

        // End one room.
        room2.end();

        // Act and Assert: Only two rooms remain active.
        assertEquals(2, repository.countActiveRooms());

        // All three rooms remain stored.
        assertEquals(3, repository.count());
    }

    @Test
    void countActiveRoomsShouldReturnZeroWhenAllRoomsAreEnded() {
        // Arrange
        Room room = new Room("ROOM001", Instant.now());

        repository.saveIfAbsent(room);
        room.end();

        // Act and Assert
        assertEquals(0, repository.countActiveRooms());
        assertEquals(1, repository.count());
    }
}
