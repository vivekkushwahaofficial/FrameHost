package com.framehost.media.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// Represents metadata for a locally hosted media session.
public record MediaSession(
        String sessionId,
        String ownerDeviceId,
        String fileName,
        String mimeType,
        long fileSizeBytes,
        Instant createdAt
        ) {

    // Validates the session metadata.
    public MediaSession {
        Objects.requireNonNull(sessionId, "Session ID is required.");
        Objects.requireNonNull(ownerDeviceId, "Owner device ID is required.");
        Objects.requireNonNull(fileName, "File name is required.");
        Objects.requireNonNull(mimeType, "MIME type is required.");
        Objects.requireNonNull(createdAt, "Creation timestamp is required.");

        if (sessionId.isBlank() || ownerDeviceId.isBlank()) {
            throw new IllegalArgumentException("Session and owner IDs cannot be blank.");
        }

        if (fileName.isBlank()
                || fileName.length() > 255
                || fileName.contains("/")
                || fileName.contains("\\")) {
            throw new IllegalArgumentException("Invalid file name.");
        }

        if (mimeType.isBlank() || mimeType.length() > 100) {
            throw new IllegalArgumentException("Invalid MIME type.");
        }

        if (fileSizeBytes <= 0) {
            throw new IllegalArgumentException("File size must be positive.");
        }
    }

    // Creates a session with a server-generated unique ID.
    public static MediaSession create(
            String ownerDeviceId,
            String fileName,
            String mimeType,
            long fileSizeBytes
    ) {
        return new MediaSession(
                UUID.randomUUID().toString(),
                ownerDeviceId,
                fileName.trim(),
                mimeType.trim(),
                fileSizeBytes,
                Instant.now()
        );
    }
}
