package com.framehost.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Request received when a device joins a room.
public record JoinRoomRequest(
        @NotBlank(message = "Device name is required")
        @Size(max = 30, message = "Device name must not exceed 30 characters")
        String deviceName
        ) {

}
