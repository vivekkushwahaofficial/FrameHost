package com.framehost.media.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.framehost.common.AuthorizationException;
import com.framehost.device.Device;
import com.framehost.device.DeviceRole;
import com.framehost.device.service.DeviceAuthenticationService;
import com.framehost.media.config.MediaStorageProperties;
import com.framehost.media.dto.CreateUploadSessionRequest;
import com.framehost.media.dto.CreateUploadSessionResponse;
import com.framehost.media.model.UploadManifest;
import com.framehost.media.model.UploadStatus;
import com.framehost.room.model.Room;
import com.framehost.room.repository.InMemoryRoomRepository;
import com.framehost.room.service.RoomNotFoundException;

@Service
public class UploadSessionService {

    private final InMemoryRoomRepository roomRepository;
    private final DeviceAuthenticationService authenticationService;
    private final FileSystemUploadStore uploadStore;
    private final MediaStorageProperties storageProperties;

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

        // Ensure the room is still active.
        if (!room.isActive()) {
            throw new IllegalStateException("Room is inactive.");
        }

        // Validate the configured upload limits.
        long maxFileSize = storageProperties.getMaxFileSizeBytes();
        int chunkSize = storageProperties.getChunkSizeBytes();
        long expiryHours = storageProperties.getUploadExpiryHours();

        if (maxFileSize <= 0 || chunkSize <= 0 || expiryHours <= 0) {
            throw new IllegalStateException(
                    "Media upload configuration is invalid."
            );
        }

        // Reject videos larger than the configured limit.
        if (request.fileSizeBytes() > maxFileSize) {
            throw new UploadLimitExceededException(
                    "Video exceeds the maximum allowed upload size."
            );
        }

        // Calculate the total number of chunks without overflow.
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
            expiresAt = createdAt.plusSeconds(
                    Math.multiplyExact(expiryHours, 3600L)
            );
        } catch (ArithmeticException exception) {
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
                java.util.List.of(),
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
}
