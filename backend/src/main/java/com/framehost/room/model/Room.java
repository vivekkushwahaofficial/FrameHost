package com.framehost.room.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.framehost.device.Device;
import com.framehost.device.DeviceRole;
import com.framehost.playback.model.PlaybackPermission;
import com.framehost.playback.model.PlaybackState;
import com.framehost.playback.model.PlaybackStatus;
import com.framehost.playback.model.VideoProvider;

// Represents a room and its participating devices.
public class Room {

    // Maximum devices allowed, including the Host.
    public static final int MAX_DEVICES = 10;

    private final String roomCode;
    private final Instant createdAt;

    // Stores the current room lifecycle state.
    private RoomStatus status = RoomStatus.ACTIVE;

    // Records when the room was terminated.
    private Instant endedAt;

    // Stores the current playback state for this room.
    private PlaybackState playbackState;

    // Stores public device information.
    private final ConcurrentHashMap<String, Device> devices
            = new ConcurrentHashMap<>();

    // Stores only hashed authentication tokens.
    private final ConcurrentHashMap<String, String> deviceTokenHashes
            = new ConcurrentHashMap<>();
    // Stores permissions separately for each participating device.
    private final ConcurrentHashMap<String, Set<PlaybackPermission>> devicePlaybackPermissions
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

    // Returns the current lifecycle state.
    public synchronized RoomStatus status() {
        return status;
    }

    // Returns the room termination timestamp.
    public synchronized Instant endedAt() {
        return endedAt;
    }

    // Checks whether the room accepts normal operations.
    public synchronized boolean isActive() {
        return status == RoomStatus.ACTIVE;
    }

    // Returns the current playback state.
    public synchronized PlaybackState playbackState() {
        return playbackState;
    }

    // Updates playback state and generates a server-controlled version.
    public synchronized PlaybackState updatePlaybackState(
            VideoProvider videoProvider,
            String videoId,
            PlaybackStatus playbackStatus,
            double currentTime,
            double playbackRate
    ) {

        if (!isActive()) {
            throw new IllegalStateException(
                    "Cannot update playback state of an inactive room."
            );
        }

        long nextVersion = playbackState == null
                ? 1
                : playbackState.stateVersion() + 1;

        PlaybackState newState = new PlaybackState(
                videoProvider,
                videoId,
                playbackStatus,
                currentTime,
                playbackRate,
                nextVersion,
                Instant.now()
        );

        playbackState = newState;

        return newState;
    }

    // Ends the room and invalidates all device credentials.
    public synchronized boolean end() {

        // Prevent repeated or invalid lifecycle transitions.
        if (status != RoomStatus.ACTIVE) {
            return false;
        }

        status = RoomStatus.ENDED;
        endedAt = Instant.now();

        // Revoke every registered device token.
        deviceTokenHashes.clear();

        // Revoke all playback permissions.
        devicePlaybackPermissions.clear();

        // Release the room's playback state.
        playbackState = null;

        return true;
    }

    // Retains the original method for internal use and test fixtures.
    public synchronized boolean addDevice(Device device) {

        if (!isActive()
                || device == null
                || devices.size() >= MAX_DEVICES) {
            return false;
        }

        if (devices.containsKey(device.deviceId())) {
            return false;
        }

        devices.put(device.deviceId(), device);

        return true;
    }

    // Adds a device and its authentication token hash atomically.
    public synchronized boolean addDevice(
            Device device,
            String tokenHash
    ) {

        if (!isActive()
                || device == null
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

        // Ended rooms cannot authenticate devices.
        if (!isActive() || tokenHash == null) {
            return null;
        }

        byte[] suppliedHash
                = tokenHash.getBytes(StandardCharsets.UTF_8);

        for (var entry : deviceTokenHashes.entrySet()) {

            byte[] storedHash
                    = entry.getValue().getBytes(StandardCharsets.UTF_8);

            if (MessageDigest.isEqual(suppliedHash, storedHash)) {
                return devices.get(entry.getKey());
            }
        }

        return null;
    }

    // Removes a device but never allows removal of the Host.
    public synchronized Device removeDevice(String deviceId) {

        if (!isActive()) {
            return null;
        }

        Device device = devices.get(deviceId);

        if (device == null || device.role() == DeviceRole.HOST) {
            return null;
        }

        deviceTokenHashes.remove(deviceId);

        devicePlaybackPermissions.remove(deviceId);

        return devices.remove(deviceId);
    }

    // Allows an authenticated non-Host device to leave.
    public synchronized Device removeOwnDevice(String deviceId) {

        if (!isActive()) {
            return null;
        }

        Device device = devices.get(deviceId);

        if (device == null || device.role() == DeviceRole.HOST) {
            return null;
        }

        // Remove authentication credentials.
        deviceTokenHashes.remove(deviceId);

        // Remove all playback permissions.
        devicePlaybackPermissions.remove(deviceId);

        // Remove the device from the room.
        return devices.remove(deviceId);
    }

    // Replaces the complete permission set for a Follower.
    public synchronized void updatePlaybackPermissions(
            String deviceId,
            Set<PlaybackPermission> permissions
    ) {

        if (!isActive()) {
            throw new IllegalStateException("Room is inactive.");
        }

        Device device = devices.get(deviceId);

        if (device == null) {
            throw new IllegalArgumentException("Device not found.");
        }

        if (device.role() == DeviceRole.HOST) {
            throw new IllegalArgumentException(
                    "Host permissions cannot be modified."
            );
        }

        if (permissions == null) {
            throw new IllegalArgumentException("Permissions are required.");
        }

        if (permissions.isEmpty()) {
            devicePlaybackPermissions.remove(deviceId);
            return;
        }

        devicePlaybackPermissions.put(deviceId, Set.copyOf(permissions));
    }

    // Checks whether a device has a particular permission.
    public synchronized boolean hasPlaybackPermission(
            String deviceId,
            PlaybackPermission permission
    ) {

        if (!isActive() || permission == null) {
            return false;
        }

        Device device = devices.get(deviceId);

        if (device == null) {
            return false;
        }

        // Host always has full playback control.
        if (device.role() == DeviceRole.HOST) {
            return true;
        }

        return devicePlaybackPermissions
                .getOrDefault(deviceId, Set.of())
                .contains(permission);
    }

    // Returns an immutable snapshot of a device's permissions.
    public synchronized Set<PlaybackPermission> getPlaybackPermissions(
            String deviceId
    ) {

        if (!isActive() || !devices.containsKey(deviceId)) {
            return Set.of();
        }

        return devicePlaybackPermissions.getOrDefault(deviceId, Set.of());
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
