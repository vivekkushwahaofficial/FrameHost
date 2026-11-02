package com.framehost.device.service;

// Thrown when a requested device does not exist in a room.
public class DeviceNotFoundException extends RuntimeException {

    public DeviceNotFoundException(String deviceId) {
        super("Device not found: " + deviceId);
    }
}
