package com.framehost.playback.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.framehost.playback.dto.PlaybackPermissionRequest;
import com.framehost.playback.dto.PlaybackPermissionResponse;
import com.framehost.playback.service.PlaybackPermissionService;

import jakarta.validation.Valid;

// Exposes playback permission management REST endpoints.
@RestController
@RequestMapping("/api/v1/rooms/{roomCode}/devices/{deviceId}/playback-permissions")
public class PlaybackPermissionController {

    private final PlaybackPermissionService permissionService;

    // Constructor injection.
    public PlaybackPermissionController(
            PlaybackPermissionService permissionService
    ) {
        this.permissionService = permissionService;
    }

    // PUT /api/v1/rooms/{roomCode}/devices/{deviceId}/playback-permissions
    // Allows the authenticated Host to replace a Follower's permissions.
    @PutMapping
    public ResponseEntity<PlaybackPermissionResponse> updatePermissions(
            @PathVariable String roomCode,
            @PathVariable String deviceId,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorizationHeader,
            @Valid @RequestBody PlaybackPermissionRequest request
    ) {

        PlaybackPermissionResponse response
                = permissionService.updatePermissions(
                        roomCode,
                        deviceId,
                        authorizationHeader,
                        request.permissions()
                );

        return ResponseEntity.ok(response);
    }
}
