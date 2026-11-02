package com.framehost.room.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.framehost.common.AuthorizationException;
import com.framehost.device.Device;
import com.framehost.device.DeviceRole;
import com.framehost.device.dto.DeviceManagementResponse;
import com.framehost.device.dto.DeviceResponse;
import com.framehost.device.service.DeviceAuthenticationService;
import com.framehost.device.service.DeviceNotFoundException;
import com.framehost.device.service.DeviceTokenService;
import com.framehost.room.dto.CreateRoomResponse;
import com.framehost.room.dto.JoinRoomRequest;
import com.framehost.room.dto.JoinRoomResponse;
import com.framehost.room.model.Room;
import com.framehost.room.repository.InMemoryRoomRepository;

// Contains the business logic for room and device management.
@Service
public class RoomService {

    private static final int MAX_GENERATION_ATTEMPTS = 5;

    private final InMemoryRoomRepository roomRepository;
    private final RoomCodeGenerator roomCodeGenerator;
    private final DeviceTokenService deviceTokenService;
    private final DeviceAuthenticationService authenticationService;

    // Constructor injection.
    public RoomService(
            InMemoryRoomRepository roomRepository,
            RoomCodeGenerator roomCodeGenerator,
            DeviceTokenService deviceTokenService,
            DeviceAuthenticationService authenticationService
    ) {
        this.roomRepository = roomRepository;
        this.roomCodeGenerator = roomCodeGenerator;
        this.deviceTokenService = deviceTokenService;
        this.authenticationService = authenticationService;
    }

    // Creates a room and registers its creator as Host.
    public CreateRoomResponse createRoom() {

        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {

            String roomCode = roomCodeGenerator.generate();

            Room room = new Room(roomCode, Instant.now());

            String hostDeviceId = UUID.randomUUID().toString();
            String deviceToken = deviceTokenService.generateToken();
            String tokenHash = deviceTokenService.hashToken(deviceToken);

            Device host = new Device(
                    hostDeviceId,
                    "Host",
                    DeviceRole.HOST,
                    Instant.now()
            );

            if (!room.addDevice(host, tokenHash)) {
                throw new IllegalStateException(
                        "Unable to register the Host device."
                );
            }

            if (roomRepository.saveIfAbsent(room)) {

                return new CreateRoomResponse(
                        room.roomCode(),
                        host.deviceId(),
                        deviceToken,
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

        Room room = findRoom(roomCode);

        String deviceId = UUID.randomUUID().toString();
        String deviceToken = deviceTokenService.generateToken();
        String tokenHash = deviceTokenService.hashToken(deviceToken);

        Device device = new Device(
                deviceId,
                request.deviceName().trim(),
                DeviceRole.FOLLOWER,
                Instant.now()
        );

        if (!room.addDevice(device, tokenHash)) {
            throw new RoomFullException(room.roomCode());
        }

        return new JoinRoomResponse(
                room.roomCode(),
                device.deviceId(),
                device.deviceName(),
                deviceToken,
                device.role(),
                device.joinedAt(),
                room.deviceCount(),
                Room.MAX_DEVICES
        );
    }

    // Returns the room's device list to its authenticated Host only.
    public List<DeviceResponse> getDevices(
            String roomCode,
            String authorizationHeader
    ) {

        Room room = findRoom(roomCode);

        Device authenticatedDevice = authenticationService.authenticate(
                room,
                authorizationHeader
        );

        requireHost(authenticatedDevice);

        return room.getDevices()
                .stream()
                .map(device -> new DeviceResponse(
                        device.deviceId(),
                        device.deviceName(),
                        device.role(),
                        device.joinedAt()
                ))
                .toList();
    }

    // Allows an authenticated non-Host device to leave its room.
    public DeviceManagementResponse leaveRoom(
            String roomCode,
            String authorizationHeader
    ) {

        Room room = findRoom(roomCode);

        Device authenticatedDevice = authenticationService.authenticate(
                room,
                authorizationHeader
        );

        if (authenticatedDevice.role() == DeviceRole.HOST) {
            throw new RoomOperationConflictException(
                    "The Host cannot leave an active room."
            );
        }

        Device removedDevice = room.removeOwnDevice(
                authenticatedDevice.deviceId()
        );

        if (removedDevice == null) {
            throw new DeviceNotFoundException(
                    authenticatedDevice.deviceId()
            );
        }

        return new DeviceManagementResponse(
                room.roomCode(),
                removedDevice.deviceId(),
                "Device left the room successfully.",
                room.deviceCount()
        );
    }

    // Allows the authenticated Host to remove another device.
    public DeviceManagementResponse removeDevice(
            String roomCode,
            String targetDeviceId,
            String authorizationHeader
    ) {

        Room room = findRoom(roomCode);

        Device authenticatedDevice = authenticationService.authenticate(
                room,
                authorizationHeader
        );

        requireHost(authenticatedDevice);

        Device targetDevice = room.findDevice(targetDeviceId);

        if (targetDevice == null) {
            throw new DeviceNotFoundException(targetDeviceId);
        }

        if (targetDevice.role() == DeviceRole.HOST) {
            throw new RoomOperationConflictException(
                    "The Host device cannot be removed."
            );
        }

        Device removedDevice = room.removeDevice(targetDeviceId);

        if (removedDevice == null) {
            throw new DeviceNotFoundException(targetDeviceId);
        }

        return new DeviceManagementResponse(
                room.roomCode(),
                removedDevice.deviceId(),
                "Device removed successfully.",
                room.deviceCount()
        );
    }

    // Finds a room using its normalized room code.
    private Room findRoom(String roomCode) {

        String normalizedRoomCode = roomCode.trim().toUpperCase();

        return roomRepository.findByRoomCode(normalizedRoomCode)
                .orElseThrow(
                        () -> new RoomNotFoundException(normalizedRoomCode)
                );
    }

    // Ensures that the authenticated device is the Host.
    private void requireHost(Device device) {

        if (device.role() != DeviceRole.HOST) {
            throw new AuthorizationException(
                    "Only the Host can perform this operation."
            );
        }
    }
}
