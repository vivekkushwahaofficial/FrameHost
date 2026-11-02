package com.framehost.room.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.framehost.device.Device;
import com.framehost.device.DeviceRole;

// Represents a room and its participating devices.
public class Room {

    // Maximum devices allowed, including the Host.
    public static final int MAX_DEVICES = 10;

    private final String roomCode;
    private final Instant createdAt;

    // Stores public device information.
    private final ConcurrentHashMap<String, Device> devices
            = new ConcurrentHashMap<>();

    // Stores only hashed authentication tokens.
    private final ConcurrentHashMap<String, String> deviceTokenHashes
            = new ConcurrentHashMap<>();

    public Room(String roomCode, Instant createdAt) {
        this.roomCode = roomCode;
        this.createdAt = createdAt;
    }

    public String roomCode() {
        return roomCode;
    }

    public Instant createdAt() {
        return createdAt;
    }

    // Retains the original method for internal use and test fixtures.
    // Devices added this way do not receive authentication credentials.
    public synchronized boolean addDevice(Device device) {

        if (device == null || devices.size() >= MAX_DEVICES) {
            return false;
        }

        if (devices.containsKey(device.deviceId())) {
            return false;
        }

        devices.put(device.deviceId(), device);

        return true;
    }

    // Adds a device and its authentication token hash atomically.
    // Production room creation and joining must use this method.
    public synchronized boolean addDevice(
            Device device,
            String tokenHash
    ) {

        if (device == null
                || tokenHash == null
                || tokenHash.isBlank()
                || devices.size() >= MAX_DEVICES) {
            return false;
        }

        if (devices.containsKey(device.deviceId())) {
            return false;
        }

        devices.put(device.deviceId(), device);
        deviceTokenHashes.put(device.deviceId(), tokenHash);

        return true;
    }

    // Finds a device by ID.
    public synchronized Device findDevice(String deviceId) {
        return devices.get(deviceId);
    }

    // Finds a device using its hashed authentication token.
    public synchronized Device findDeviceByTokenHash(String tokenHash) {

        if (tokenHash == null) {
            return null;
        }

        byte[] suppliedHash = tokenHash.getBytes(StandardCharsets.UTF_8);

        for (var entry : deviceTokenHashes.entrySet()) {

            byte[] storedHash = entry.getValue()
                    .getBytes(StandardCharsets.UTF_8);

            if (MessageDigest.isEqual(suppliedHash, storedHash)) {
                return devices.get(entry.getKey());
            }
        }

        return null;
    }

    // Removes a device but never allows removal of the Host.
    public synchronized Device removeDevice(String deviceId) {

        Device device = devices.get(deviceId);

        if (device == null || device.role() == DeviceRole.HOST) {
            return null;
        }

        deviceTokenHashes.remove(deviceId);

        return devices.remove(deviceId);
    }

    // Allows an authenticated non-Host device to leave.
    public synchronized Device removeOwnDevice(String deviceId) {

        Device device = devices.get(deviceId);

        if (device == null || device.role() == DeviceRole.HOST) {
            return null;
        }

        deviceTokenHashes.remove(deviceId);

        return devices.remove(deviceId);
    }

    // Returns a snapshot of participating devices.
    public synchronized List<Device> getDevices() {
        return List.copyOf(devices.values());
    }

    // Returns the current number of devices.
    public synchronized int deviceCount() {
        return devices.size();
    }
}
