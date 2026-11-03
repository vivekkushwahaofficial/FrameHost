package com.framehost.playback.dto;

import java.util.Set;

import com.framehost.playback.model.PlaybackPermission;

// Represents the result of a permission management operation.
public record PlaybackPermissionResponse(
        String roomCode,
        String deviceId,
        Set<PlaybackPermission> permissions,
        String message
        ) {}
