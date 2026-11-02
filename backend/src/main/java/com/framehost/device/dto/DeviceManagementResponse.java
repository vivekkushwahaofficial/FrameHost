
package com.framehost.device.dto;

// Response returned after a device management operation.
public record DeviceManagementResponse(
        String roomCode,
        String deviceId,
        String message,
        int deviceCount
) {
}
