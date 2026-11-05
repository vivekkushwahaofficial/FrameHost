package com.framehost.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// Request containing metadata for a locally hosted video.
public record CreateMediaSessionRequest(
        @NotBlank(message = "File name is required.")
        @Size(max = 255, message = "File name cannot exceed 255 characters.")
        String fileName,
        @NotBlank(message = "MIME type is required.")
        @Size(max = 100, message = "MIME type cannot exceed 100 characters.")
        @Pattern(
                regexp = "^video/[A-Za-z0-9.+-]+$",
                message = "A valid video MIME type is required."
        )
        String mimeType,
        @Positive(message = "File size must be positive.")
        long fileSizeBytes
        ) {

}
