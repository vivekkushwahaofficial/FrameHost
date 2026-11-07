package com.framehost.media.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.framehost.media.config.MediaStorageProperties;
import com.framehost.media.model.UploadManifest;
import com.framehost.media.model.UploadStatus;

@Component
public class FileSystemUploadStore {

    private static final String MANIFEST_FILE = "manifest.properties";
    private static final String CHUNKS_DIRECTORY = "chunks";

    private final Path storageRoot;

    public FileSystemUploadStore(MediaStorageProperties properties) {
        this.storageRoot = properties.getStorageDir()
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(storageRoot);
        } catch (IOException exception) {
            throw new UploadStorageException(
                    "Could not initialize media storage directory.",
                    exception
            );
        }
    }

    // Save upload metadata to the filesystem.
    public void save(UploadManifest manifest) {
        Path uploadDirectory = getUploadDirectory(manifest.uploadId());

        try {
            Files.createDirectories(uploadDirectory);
            Files.createDirectories(
                    uploadDirectory.resolve(CHUNKS_DIRECTORY)
            );

            Properties properties = toProperties(manifest);

            Path temporaryFile = Files.createTempFile(
                    uploadDirectory,
                    "manifest-",
                    ".tmp"
            );

            try {
                try (OutputStream output = Files.newOutputStream(temporaryFile)) {
                    properties.store(output, "FrameHost upload manifest");
                }

                moveAtomically(
                        temporaryFile,
                        uploadDirectory.resolve(MANIFEST_FILE)
                );
            } finally {
                Files.deleteIfExists(temporaryFile);
            }

        } catch (IOException exception) {
            throw new UploadStorageException(
                    "Could not save upload manifest.",
                    exception
            );
        }
    }

    // Read upload metadata from the filesystem.
    public Optional<UploadManifest> read(UUID uploadId) {
        Path manifestPath = getUploadDirectory(uploadId)
                .resolve(MANIFEST_FILE);

        if (!Files.isRegularFile(manifestPath)) {
            return Optional.empty();
        }

        try (InputStream input = Files.newInputStream(manifestPath)) {
            Properties properties = new Properties();
            properties.load(input);

            return Optional.of(fromProperties(properties));

        } catch (IOException | RuntimeException exception) {
            throw new UploadStorageException(
                    "Could not read upload manifest.",
                    exception
            );
        }
    }

    // Return the directory in which video chunks will be stored.
    public Path chunkDirectory(UUID uploadId) {
        return getUploadDirectory(uploadId)
                .resolve(CHUNKS_DIRECTORY);
    }

    // Delete an upload and all its stored files.
    public void delete(UUID uploadId) {
        Path uploadDirectory = getUploadDirectory(uploadId);

        if (!Files.exists(uploadDirectory)) {
            return;
        }

        try {
            deleteRecursively(uploadDirectory);
        } catch (IOException exception) {
            throw new UploadStorageException(
                    "Could not delete upload data.",
                    exception
            );
        }
    }

    // Convert an upload manifest into filesystem properties.
    private Properties toProperties(UploadManifest manifest) {
        Properties properties = new Properties();

        properties.setProperty("uploadId", manifest.uploadId().toString());
        properties.setProperty("roomCode", manifest.roomCode());
        properties.setProperty("ownerDeviceId", manifest.ownerDeviceId());
        properties.setProperty("fileName", manifest.fileName());
        properties.setProperty("mimeType", manifest.mimeType());
        properties.setProperty("fileSizeBytes", String.valueOf(manifest.fileSizeBytes()));
        properties.setProperty("chunkSizeBytes", String.valueOf(manifest.chunkSizeBytes()));
        properties.setProperty("totalChunks", String.valueOf(manifest.totalChunks()));

        properties.setProperty(
                "uploadedChunkIndexes",
                manifest.uploadedChunkIndexes().stream()
                        .map(String::valueOf)
                        .reduce((first, second) -> first + "," + second)
                        .orElse("")
        );

        properties.setProperty(
                "contiguousUploadedBytes",
                String.valueOf(manifest.contiguousUploadedBytes())
        );

        properties.setProperty("status", manifest.status().name());
        properties.setProperty("createdAt", manifest.createdAt().toString());
        properties.setProperty("expiresAt", manifest.expiresAt().toString());

        return properties;
    }

    // Convert filesystem properties back into an upload manifest.
    private UploadManifest fromProperties(Properties properties) {
        String indexes = properties.getProperty("uploadedChunkIndexes", "");

        List<Integer> uploadedChunks = new ArrayList<>();

        if (!indexes.isBlank()) {
            for (String index : indexes.split(",")) {
                uploadedChunks.add(Integer.valueOf(index));
            }
        }

        return new UploadManifest(
                UUID.fromString(properties.getProperty("uploadId")),
                properties.getProperty("roomCode"),
                properties.getProperty("ownerDeviceId"),
                properties.getProperty("fileName"),
                properties.getProperty("mimeType"),
                Long.parseLong(properties.getProperty("fileSizeBytes")),
                Integer.parseInt(properties.getProperty("chunkSizeBytes")),
                Integer.parseInt(properties.getProperty("totalChunks")),
                uploadedChunks,
                Long.parseLong(properties.getProperty("contiguousUploadedBytes")),
                UploadStatus.valueOf(properties.getProperty("status")),
                Instant.parse(properties.getProperty("createdAt")),
                Instant.parse(properties.getProperty("expiresAt"))
        );
    }

    // Resolve an upload directory using a server-generated UUID.
    private Path getUploadDirectory(UUID uploadId) {
        if (uploadId == null) {
            throw new IllegalArgumentException("Upload ID cannot be null.");
        }

        return storageRoot.resolve(uploadId.toString()).normalize();
    }

    // Move a temporary manifest into its final location.
    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(
                    source,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(
                    source,
                    target,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    // Delete files and directories from the deepest level upward.
    private void deleteRecursively(Path directory) throws IOException {

        // Collect paths before deleting them.
        List<Path> paths;

        try (var walk = Files.walk(directory)) {
            paths = walk.sorted(
                    java.util.Comparator.reverseOrder()
            ).toList();
        }

        // Delete children first, then the parent directory.
        for (Path path : paths) {
            Files.deleteIfExists(path);
        }
    }
}
