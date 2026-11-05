package com.framehost.room.dto;

import java.time.Instant;

import com.framehost.media.model.MediaSession;

// Response containing the created media session metadata.
public record MediaSessionResponse(
        String roomCode,
        String sessionId,
        String ownerDeviceId,
        String fileName,
        String mimeType,
        long fileSizeBytes,
        Instant createdAt
        ) {

    // Converts the domain model into an API response.
    public static MediaSessionResponse from(
            String roomCode,
            MediaSession session
    ) {
        return new MediaSessionResponse(
                roomCode,
                session.sessionId(),
                session.ownerDeviceId(),
                session.fileName(),
                session.mimeType(),
                session.fileSizeBytes(),
                session.createdAt()
        );
    }
}
