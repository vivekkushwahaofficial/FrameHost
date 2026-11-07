package com.framehost.media.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "framehost.media")
public class MediaStorageProperties {

    // Directory where uploaded video files will be stored.
    private Path storageDir = Path.of("./data/media");

    // Maximum allowed video size: 2 GB.
    private long maxFileSizeBytes = 2L * 1024 * 1024 * 1024;

    // Each upload chunk will be 4 MB.
    private int chunkSizeBytes = 4 * 1024 * 1024;

    // Upload sessions expire after 24 hours.
    private long uploadExpiryHours = 24;

    public Path getStorageDir() {
        return storageDir;
    }

    public void setStorageDir(Path storageDir) {
        this.storageDir = storageDir;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public int getChunkSizeBytes() {
        return chunkSizeBytes;
    }

    public void setChunkSizeBytes(int chunkSizeBytes) {
        this.chunkSizeBytes = chunkSizeBytes;
    }

    public long getUploadExpiryHours() {
        return uploadExpiryHours;
    }

    public void setUploadExpiryHours(long uploadExpiryHours) {
        this.uploadExpiryHours = uploadExpiryHours;
    }
}
