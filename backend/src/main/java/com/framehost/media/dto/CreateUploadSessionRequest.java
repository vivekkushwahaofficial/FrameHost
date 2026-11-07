package com.framehost.media.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// Contains the metadata required to initiate a video upload.
public record CreateUploadSessionRequest(
        // Original video filename.
        @NotBlank(message = "File name is required.")
        @Size(max = 255, message = "File name cannot exceed 255 characters.")
        @Pattern(
                regexp = "^(?!\\.\\.?$)[^/\\\\\\\\]+$",
                message = "Invalid file name."
        )
        String fileName,
        // Only video MIME types are accepted.
        @NotBlank(message = "MIME type is required.")
        @Size(max = 100, message = "MIME type cannot exceed 100 characters.")
        @Pattern(
                regexp = "^video/[A-Za-z0-9][A-Za-z0-9.+-]*$",
                message = "Only video MIME types are accepted."
        )
        String mimeType,
        // Total video size in bytes.
        @Positive(message = "File size must be positive.")
        long fileSizeBytes
        ) {

}
