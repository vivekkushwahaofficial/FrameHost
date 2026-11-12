package com.framehost.media.service;

import java.io.InputStream;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.framehost.common.AuthorizationException;
import com.framehost.device.Device;
import com.framehost.device.DeviceRole;
import com.framehost.device.service.DeviceAuthenticationService;
import com.framehost.media.config.MediaStorageProperties;
import com.framehost.media.dto.CreateUploadSessionRequest;
import com.framehost.media.dto.CreateUploadSessionResponse;
import com.framehost.media.dto.UploadChunkResponse;
import com.framehost.media.model.UploadManifest;
import com.framehost.media.model.UploadStatus;
import com.framehost.room.model.Room;
import com.framehost.room.repository.InMemoryRoomRepository;
import com.framehost.room.service.RoomNotFoundException;

@Service
public class UploadSessionService {

    private static final int LOCK_COUNT = 64;

    private final InMemoryRoomRepository roomRepository;
    private final DeviceAuthenticationService authenticationService;
    private final FileSystemUploadStore uploadStore;
    private final MediaStorageProperties storageProperties;

    // Fixed locks prevent an unbounded lock map.
    private final Object[] uploadLocks = createUploadLocks();

    public UploadSessionService(
            InMemoryRoomRepository roomRepository,
            DeviceAuthenticationService authenticationService,
            FileSystemUploadStore uploadStore,
            MediaStorageProperties storageProperties
    ) {
        this.roomRepository = roomRepository;
        this.authenticationService = authenticationService;
        this.uploadStore = uploadStore;
        this.storageProperties = storageProperties;
    }

    // Creates an upload session for an authenticated Host.
    public CreateUploadSessionResponse createUploadSession(
            String roomCode,
            CreateUploadSessionRequest request,
            String authorizationHeader
    ) {

        if (roomCode == null || roomCode.isBlank()) {
            throw new IllegalArgumentException("Room code is required.");
        }

        if (request == null) {
            throw new IllegalArgumentException(
                    "Upload request is required."
            );
        }

        String normalizedRoomCode = roomCode.trim().toUpperCase();

        // Find the requested room.
        Room room = roomRepository.findByRoomCode(normalizedRoomCode)
                .orElseThrow(
                        () -> new RoomNotFoundException(normalizedRoomCode)
                );

        // Authenticate the requesting device.
        Device device = authenticationService.authenticate(
                room,
                authorizationHeader
        );

        // Only the Host can initiate an upload.
        if (device.role() != DeviceRole.HOST) {
            throw new AuthorizationException(
                    "Only the Host can upload local media."
            );
        }

        // Ensure the room is active.
        if (!room.isActive()) {
            throw new IllegalStateException("Room is inactive.");
        }

        // Validate configured upload limits.
        long maxFileSize = storageProperties.getMaxFileSizeBytes();
        int chunkSize = storageProperties.getChunkSizeBytes();
        long expiryHours = storageProperties.getUploadExpiryHours();

        if (maxFileSize <= 0 || chunkSize <= 0 || expiryHours <= 0) {
            throw new IllegalStateException(
                    "Media upload configuration is invalid."
            );
        }

        // Reject files exceeding the configured limit.
        if (request.fileSizeBytes() <= 0
                || request.fileSizeBytes() > maxFileSize) {
            throw new UploadLimitExceededException(
                    "Invalid video size or maximum upload size exceeded."
            );
        }

        // Calculate the total number of chunks safely.
        long totalChunksLong
                = ((request.fileSizeBytes() - 1) / chunkSize) + 1;

        if (totalChunksLong > Integer.MAX_VALUE) {
            throw new UploadLimitExceededException(
                    "Video requires too many upload chunks."
            );
        }

        int totalChunks = (int) totalChunksLong;

        // Generate server-controlled upload metadata.
        UUID uploadId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        Instant expiresAt;

        try {
            long expirySeconds = Math.multiplyExact(expiryHours, 3600L);
            expiresAt = createdAt.plusSeconds(expirySeconds);

        } catch (ArithmeticException | DateTimeException exception) {
            throw new IllegalStateException(
                    "Upload expiry configuration is too large.",
                    exception
            );
        }

        UploadManifest manifest = new UploadManifest(
                uploadId,
                normalizedRoomCode,
                device.deviceId(),
                request.fileName().trim(),
                request.mimeType().trim(),
                request.fileSizeBytes(),
                chunkSize,
                totalChunks,
                List.of(),
                0L,
                UploadStatus.INITIATED,
                createdAt,
                expiresAt
        );

        // Persist the upload metadata.
        uploadStore.save(manifest);

        // Return the upload session details.
        return new CreateUploadSessionResponse(
                normalizedRoomCode,
                uploadId,
                manifest.fileName(),
                manifest.mimeType(),
                manifest.fileSizeBytes(),
                chunkSize,
                totalChunks,
                createdAt,
                expiresAt
        );
    }

    // Accept and store one chunk for an existing upload.
    public UploadChunkResponse uploadChunk(
            String roomCode,
            UUID uploadId,
            int chunkIndex,
            long contentLength,
            InputStream inputStream,
            String authorizationHeader
    ) {

        if (roomCode == null || roomCode.isBlank()) {
            throw new IllegalArgumentException("Room code is required.");
        }

        if (uploadId == null) {
            throw new IllegalArgumentException("Upload ID is required.");
        }

        if (inputStream == null) {
            throw new InvalidUploadChunkException(
                    "Chunk input stream cannot be null."
            );
        }

        String normalizedRoomCode = roomCode.trim().toUpperCase();

        // Find the requested room.
        Room room = roomRepository.findByRoomCode(normalizedRoomCode)
                .orElseThrow(
                        () -> new RoomNotFoundException(normalizedRoomCode)
                );

        // Authenticate the requesting device.
        Device device = authenticationService.authenticate(
                room,
                authorizationHeader
        );

        // Ensure the room is active.
        if (!room.isActive()) {
            throw new IllegalStateException("Room is inactive.");
        }

        // Only the Host can upload chunks.
        if (device.role() != DeviceRole.HOST) {
            throw new AuthorizationException(
                    "Only the Host can upload media chunks."
            );
        }

        // Use a fixed lock selected by upload ID.
        Object uploadLock = getUploadLock(uploadId);

        synchronized (uploadLock) {
            return processChunk(
                    normalizedRoomCode,
                    device,
                    uploadId,
                    chunkIndex,
                    contentLength,
                    inputStream
            );
        }
    }

    // Validate the upload and persist the chunk.
    private UploadChunkResponse processChunk(
            String roomCode,
            Device device,
            UUID uploadId,
            int chunkIndex,
            long contentLength,
            InputStream inputStream
    ) {

        // Read the upload manifest.
        UploadManifest manifest = uploadStore.read(uploadId)
                .orElseThrow(
                        () -> new UploadSessionNotFoundException(
                                "Upload session not found: " + uploadId
                        )
                );

        // Verify that this upload belongs to the requested room.
        if (!manifest.roomCode().equals(roomCode)) {
            throw new AuthorizationException(
                    "This upload does not belong to the requested room."
            );
        }

        // Only the device that created the upload can send its chunks.
        if (!manifest.ownerDeviceId().equals(device.deviceId())) {
            throw new AuthorizationException(
                    "You do not own this upload session."
            );
        }

        // Reject expired uploads.
        if (!Instant.now().isBefore(manifest.expiresAt())) {
            throw new UploadConflictException(
                    "This upload session has expired."
            );
        }

        // Only initiated or actively uploading sessions accept chunks.
        if (manifest.status() != UploadStatus.INITIATED
                && manifest.status() != UploadStatus.UPLOADING) {
            throw new UploadConflictException(
                    "This upload session cannot accept more chunks."
            );
        }

        // Validate the chunk index.
        if (chunkIndex < 0 || chunkIndex >= manifest.totalChunks()) {
            throw new InvalidUploadChunkException(
                    "Invalid chunk index."
            );
        }

        // Calculate the expected size for this chunk.
        long chunkOffset
                = (long) chunkIndex * manifest.chunkSizeBytes();

        long expectedBytes = Math.min(
                manifest.chunkSizeBytes(),
                manifest.fileSizeBytes() - chunkOffset
        );

        // Require the exact Content-Length.
        if (contentLength != expectedBytes) {
            throw new InvalidUploadChunkException(
                    "Invalid chunk size. Expected "
                    + expectedBytes
                    + " bytes."
            );
        }

        // Prevent duplicate chunk uploads.
        if (manifest.uploadedChunkIndexes().contains(chunkIndex)
                || uploadStore.chunkExists(uploadId, chunkIndex)) {
            throw new UploadConflictException(
                    "This chunk has already been uploaded."
            );
        }

        // Store the chunk safely.
        uploadStore.writeChunk(
                uploadId,
                chunkIndex,
                inputStream,
                expectedBytes
        );

        // Build the updated uploaded-chunk list.
        List<Integer> uploadedIndexes = new ArrayList<>(
                manifest.uploadedChunkIndexes()
        );

        uploadedIndexes.add(chunkIndex);
        uploadedIndexes.sort(
                (first, second) -> Integer.compare(first, second)
        );

        // Calculate consecutive uploaded bytes starting at chunk zero.
        Set<Integer> uploadedIndexSet = new HashSet<>(uploadedIndexes);

        int nextExpectedIndex = 0;

        while (uploadedIndexSet.contains(nextExpectedIndex)) {
            nextExpectedIndex++;
        }

        long contiguousBytes = Math.min(
                (long) nextExpectedIndex * manifest.chunkSizeBytes(),
                manifest.fileSizeBytes()
        );

        // Create the updated manifest.
        UploadManifest updatedManifest = new UploadManifest(
                manifest.uploadId(),
                manifest.roomCode(),
                manifest.ownerDeviceId(),
                manifest.fileName(),
                manifest.mimeType(),
                manifest.fileSizeBytes(),
                manifest.chunkSizeBytes(),
                manifest.totalChunks(),
                uploadedIndexes,
                contiguousBytes,
                UploadStatus.UPLOADING,
                manifest.createdAt(),
                manifest.expiresAt()
        );

        // Persist progress after storing the chunk.
        try {
            uploadStore.save(updatedManifest);

        } catch (RuntimeException exception) {

            // Remove the newly written chunk if manifest saving fails.
            try {
                uploadStore.deleteChunk(uploadId, chunkIndex);

            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }

            throw exception;
        }

        // Return updated upload progress.
        return new UploadChunkResponse(
                uploadId,
                chunkIndex,
                uploadedIndexes.size(),
                manifest.totalChunks(),
                contiguousBytes,
                UploadStatus.UPLOADING
        );
    }

    // Select a lock consistently for each upload ID.
    private Object getUploadLock(UUID uploadId) {
        int lockIndex = Math.floorMod(
                uploadId.hashCode(),
                LOCK_COUNT
        );

        return uploadLocks[lockIndex];
    }

    // Create a fixed set of locks.
    private static Object[] createUploadLocks() {
        Object[] locks = new Object[LOCK_COUNT];

        for (int index = 0; index < locks.length; index++) {
            locks[index] = new Object();
        }

        return locks;
    }
}
