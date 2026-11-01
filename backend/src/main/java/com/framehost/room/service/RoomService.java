package com.framehost.room.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.framehost.device.Device;
import com.framehost.device.DeviceRole;
import com.framehost.room.dto.CreateRoomResponse;
import com.framehost.room.dto.JoinRoomRequest;
import com.framehost.room.dto.JoinRoomResponse;
import com.framehost.room.model.Room;
import com.framehost.room.repository.InMemoryRoomRepository;

// Contains the business logic for creating and joining rooms.
@Service
public class RoomService {

    private static final int MAX_GENERATION_ATTEMPTS = 5;

    private final InMemoryRoomRepository roomRepository;
    private final RoomCodeGenerator roomCodeGenerator;

    // Constructor injection.
    public RoomService(
            InMemoryRoomRepository roomRepository,
            RoomCodeGenerator roomCodeGenerator
    ) {
        this.roomRepository = roomRepository;
        this.roomCodeGenerator = roomCodeGenerator;
    }

    // Creates a room and registers its creator as Host.
    public CreateRoomResponse createRoom() {

        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {

            String roomCode = roomCodeGenerator.generate();

            Room room = new Room(roomCode, Instant.now());

            // Generate the Host device ID.
            String hostDeviceId = UUID.randomUUID().toString();

            // Create the Host device.
            Device host = new Device(
                    hostDeviceId,
                    "Host",
                    DeviceRole.HOST,
                    Instant.now()
            );

            // Register the Host as the first room participant.
            room.addDevice(host);

            // Save the room only if its code is unique.
            if (roomRepository.saveIfAbsent(room)) {

                return new CreateRoomResponse(
                        room.roomCode(),
                        host.deviceId(),
                        host.role(),
                        room.createdAt(),
                        room.deviceCount(),
                        Room.MAX_DEVICES
                );
            }
        }

        throw new IllegalStateException(
                "Unable to generate a unique room code. Please try again."
        );
    }

    // Joins an existing room as a Follower.
    public JoinRoomResponse joinRoom(
            String roomCode,
            JoinRoomRequest request
    ) {

        // Normalize the room code.
        String normalizedRoomCode = roomCode.trim().toUpperCase();

        // Find the requested room.
        Room room = roomRepository.findByRoomCode(normalizedRoomCode)
                .orElseThrow(
                        () -> new RoomNotFoundException(normalizedRoomCode)
                );

        // Generate a unique device ID.
        String deviceId = UUID.randomUUID().toString();

        // Create a device with the default Follower role.
        Device device = new Device(
                deviceId,
                request.deviceName().trim(),
                DeviceRole.FOLLOWER,
                Instant.now()
        );

        // Add the device if room capacity is available.
        if (!room.addDevice(device)) {
            throw new RoomFullException(normalizedRoomCode);
        }

        // Return the joining device information.
        return new JoinRoomResponse(
                room.roomCode(),
                device.deviceId(),
                device.deviceName(),
                device.role(),
                device.joinedAt(),
                room.deviceCount(),
                Room.MAX_DEVICES
        );
    }
}
