package com.framehost.media.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.framehost.media.config.MediaStorageProperties;
import com.framehost.media.model.UploadManifest;
import com.framehost.media.model.UploadStatus;

class FileSystemUploadStoreTest {

    @TempDir
    Path tempDir;

    private FileSystemUploadStore uploadStore;

    @BeforeEach
    public void setUp() {
        MediaStorageProperties properties = new MediaStorageProperties();

        // Use a temporary directory instead of real media storage.
        properties.setStorageDir(tempDir);

        uploadStore = new FileSystemUploadStore(properties);
    }

    @Test
    void shouldSaveAndReadUploadManifest() {
        // Create test upload metadata.
        UploadManifest manifest = createManifest();

        // Save the manifest.
        uploadStore.save(manifest);

        // Read the saved manifest.
        Optional<UploadManifest> result
                = uploadStore.read(manifest.uploadId());

        // Verify the saved data matches the original.
        assertTrue(result.isPresent());
        assertEquals(manifest, result.get());
    }

    @Test
    void shouldCreateChunkDirectory() {
        UUID uploadId = UUID.randomUUID();

        // Get the chunk directory path.
        Path chunkDirectory = uploadStore.chunkDirectory(uploadId);

        // Create the upload structure.
        uploadStore.save(createManifest(uploadId));

        // Verify that the directory exists.
        assertTrue(Files.isDirectory(chunkDirectory));
    }

    @Test
    void shouldDeleteUploadDirectory() throws Exception {
        UploadManifest manifest = createManifest();

        // Save the upload.
        uploadStore.save(manifest);

        // Delete the upload.
        uploadStore.delete(manifest.uploadId());

        // Verify that its directory no longer exists.
        assertFalse(
                Files.exists(tempDir.resolve(manifest.uploadId().toString()))
        );

        // Verify that its manifest cannot be found.
        assertTrue(uploadStore.read(manifest.uploadId()).isEmpty());
    }

    private UploadManifest createManifest() {
        return createManifest(UUID.randomUUID());
    }

    private UploadManifest createManifest(UUID uploadId) {
        Instant now = Instant.now();

        return new UploadManifest(
                uploadId,
                "ABC123",
                "device-123",
                "sample.mp4",
                "video/mp4",
                10_485_760L,
                4_194_304,
                3,
                List.of(),
                0L,
                UploadStatus.INITIATED,
                now,
                now.plusSeconds(86_400)
        );
    }
}
