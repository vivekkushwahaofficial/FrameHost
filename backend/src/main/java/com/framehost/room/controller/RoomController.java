package com.framehost.room.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.framehost.device.dto.DeviceManagementResponse;
import com.framehost.device.dto.DeviceResponse;
import com.framehost.room.dto.CreateRoomResponse;
import com.framehost.room.dto.EndRoomResponse;
import com.framehost.room.dto.JoinRoomRequest;
import com.framehost.room.dto.JoinRoomResponse;
import com.framehost.room.service.RoomService;

import jakarta.validation.Valid;

// Exposes room and device management REST endpoints.
@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;

    // Constructor injection.
    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    // POST /api/v1/rooms
    // Creates a room and registers its creator as Host.
    @PostMapping
    public ResponseEntity<CreateRoomResponse> createRoom() {

        CreateRoomResponse response = roomService.createRoom();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // POST /api/v1/rooms/{roomCode}/join
    // Joins an existing room as a Follower.
    @PostMapping("/{roomCode}/join")
    public ResponseEntity<JoinRoomResponse> joinRoom(
            @PathVariable String roomCode,
            @Valid @RequestBody JoinRoomRequest request
    ) {

        JoinRoomResponse response = roomService.joinRoom(
                roomCode,
                request
        );

        return ResponseEntity.ok(response);
    }

    // GET /api/v1/rooms/{roomCode}/devices
    // Returns the device list to the authenticated Host only.
    @GetMapping("/{roomCode}/devices")
    public ResponseEntity<List<DeviceResponse>> getDevices(
            @PathVariable String roomCode,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorizationHeader
    ) {

        List<DeviceResponse> devices = roomService.getDevices(
                roomCode,
                authorizationHeader
        );

        return ResponseEntity.ok(devices);
    }

    // POST /api/v1/rooms/{roomCode}/leave
    // Allows an authenticated non-Host device to leave.
    @PostMapping("/{roomCode}/leave")
    public ResponseEntity<DeviceManagementResponse> leaveRoom(
            @PathVariable String roomCode,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorizationHeader
    ) {

        DeviceManagementResponse response = roomService.leaveRoom(
                roomCode,
                authorizationHeader
        );

        return ResponseEntity.ok(response);
    }

    // DELETE /api/v1/rooms/{roomCode}/devices/{deviceId}
    // Allows the authenticated Host to remove another device.
    @DeleteMapping("/{roomCode}/devices/{deviceId}")
    public ResponseEntity<DeviceManagementResponse> removeDevice(
            @PathVariable String roomCode,
            @PathVariable String deviceId,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorizationHeader
    ) {

        DeviceManagementResponse response = roomService.removeDevice(
                roomCode,
                deviceId,
                authorizationHeader
        );

        return ResponseEntity.ok(response);
    }

    // DELETE /api/v1/rooms/{roomCode}
    // Allows the authenticated Host to end the room.
    @DeleteMapping("/{roomCode}")
    public ResponseEntity<EndRoomResponse> endRoom(
            @PathVariable String roomCode,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorizationHeader
    ) {

        EndRoomResponse response = roomService.endRoom(
                roomCode,
                authorizationHeader
        );

        return ResponseEntity.ok(response);
    }

}
