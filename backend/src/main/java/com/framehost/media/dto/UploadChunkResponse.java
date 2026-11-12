package com.framehost.media.dto;

import java.util.UUID;

import com.framehost.media.model.UploadStatus;

public record UploadChunkResponse(
        UUID uploadId,
        int chunkIndex,
        int uploadedChunkCount,
        int totalChunks,
        long contiguousUploadedBytes,
        UploadStatus status
        ) {

}
