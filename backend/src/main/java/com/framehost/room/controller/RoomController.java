package com.framehost.room.controller;

import com.framehost.room.dto.CreateRoomResponse;
import com.framehost.room.dto.JoinRoomRequest;
import com.framehost.room.dto.JoinRoomResponse;
import com.framehost.room.service.RoomService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Exposes room-related REST endpoints.
@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    // POST /api/v1/rooms
    @PostMapping
    public ResponseEntity<CreateRoomResponse> createRoom() {

        CreateRoomResponse response = roomService.createRoom();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // POST /api/v1/rooms/{roomCode}/join
    @PostMapping("/{roomCode}/join")
    public ResponseEntity<JoinRoomResponse> joinRoom(
            @PathVariable String roomCode,
            @Valid @RequestBody JoinRoomRequest request
    ) {

        JoinRoomResponse response = roomService.joinRoom(
                roomCode,
                request
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}
