package com.framehost.media.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Stores the metadata and progress of one video upload.
public record UploadManifest(
        // Unique identifier for this upload.
        UUID uploadId,
        // Room in which the video will be shared.
        String roomCode,
        // Device that initiated the upload.
        String ownerDeviceId,
        // Original video filename.
        String fileName,
        // Video MIME type, for example video/mp4.
        String mimeType,
        // Total video size in bytes.
        long fileSizeBytes,
        // Expected size of each upload chunk.
        int chunkSizeBytes,
        // Total number of chunks expected.
        int totalChunks,
        // Indexes of successfully uploaded chunks.
        List<Integer> uploadedChunkIndexes,
        // Number of consecutive bytes uploaded from the beginning.
        long contiguousUploadedBytes,
        // Current upload state.
        UploadStatus status,
        // When this upload session was created.
        Instant createdAt,
        // When this upload session expires.
        Instant expiresAt
        ) {

    // Prevent callers from modifying the original chunk list.
    public UploadManifest {
        uploadedChunkIndexes = List.copyOf(uploadedChunkIndexes);
    }
}
