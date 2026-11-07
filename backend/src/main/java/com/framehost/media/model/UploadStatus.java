package com.framehost.media.model;

// Represents the lifecycle of a video upload.
public enum UploadStatus {

    // Upload session created, but no chunks uploaded yet.
    INITIATED,
    // At least one chunk has been uploaded.
    UPLOADING,
    // All chunks uploaded and final video verified.
    COMPLETED,
    // Upload session has expired.
    EXPIRED,
    // Upload failed because of an unrecoverable error.
    FAILED
}
