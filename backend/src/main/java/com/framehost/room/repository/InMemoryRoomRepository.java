
package com.framehost.room.repository;

import com.framehost.room.model.Room;

import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// Provides thread-safe in-memory storage for rooms.
@Repository
public class InMemoryRoomRepository {

    // Stores rooms using their unique room codes.
    private final ConcurrentHashMap<String, Room> rooms =
            new ConcurrentHashMap<>();

    // Saves a room only when its code is not already present.
    public boolean saveIfAbsent(Room room) {
        return rooms.putIfAbsent(room.roomCode(), room) == null;
    }

    // Finds a room using its code.
    public Optional<Room> findByRoomCode(String roomCode) {
        return Optional.ofNullable(rooms.get(roomCode));
    }

    // Returns the total number of stored rooms.
    public int count() {
        return rooms.size();
    }
}
