package com.framehost.room.model;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.framehost.device.Device;

// Represents a room and its participating devices.
public class Room {

    // Maximum devices allowed, including the Host.
    public static final int MAX_DEVICES = 10;

    private final String roomCode;
    private final Instant createdAt;

    // Thread-safe storage for participating devices.
    private final ConcurrentHashMap<String, Device> devices
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

    // Adds a device if capacity is available.
    public synchronized boolean addDevice(Device device) {
        if (devices.size() >= MAX_DEVICES) {
            return false;
        }

        return devices.putIfAbsent(device.deviceId(), device) == null;
    }

    // Finds a device by ID.
    public Device findDevice(String deviceId) {
        return devices.get(deviceId);
    }

    // Returns a snapshot of participating devices.
    public List<Device> getDevices() {
        return List.copyOf(devices.values());
    }

    // Returns the current number of devices.
    public int deviceCount() {
        return devices.size();
    }
}
