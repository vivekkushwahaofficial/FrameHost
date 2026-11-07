package com.framehost.media.dto;

import java.time.Instant;
import java.util.UUID;

// Contains the details returned after creating an upload session.
public record CreateUploadSessionResponse(
        String roomCode,
        UUID uploadId,
        String fileName,
        String mimeType,
        long fileSizeBytes,
        int chunkSizeBytes,
        int totalChunks,
        Instant createdAt,
        Instant expiresAt
        ) {

}
