package com.framehost.playback.dto;

import java.util.Set;

import com.framehost.playback.model.PlaybackPermission;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Represents the complete permission set assigned to a Follower.
public record PlaybackPermissionRequest(
        @NotNull(message = "Permissions are required.")
        @Size(max = 3, message = "Maximum three permissions are allowed.")
        Set<PlaybackPermission> permissions
        ) {}
