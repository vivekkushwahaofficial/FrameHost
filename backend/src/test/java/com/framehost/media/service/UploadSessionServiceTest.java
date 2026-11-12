package com.framehost.media.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.framehost.common.AuthenticationException;
import com.framehost.common.AuthorizationException;
import com.framehost.device.Device;
import com.framehost.device.DeviceRole;
import com.framehost.device.service.DeviceAuthenticationService;
import com.framehost.media.config.MediaStorageProperties;
import com.framehost.media.dto.CreateUploadSessionRequest;
import com.framehost.media.dto.CreateUploadSessionResponse;
import com.framehost.media.dto.UploadChunkResponse;
import com.framehost.media.model.MediaSession;
import com.framehost.media.model.UploadManifest;
import com.framehost.media.model.UploadStatus;
import com.framehost.room.model.Room;
import com.framehost.room.repository.InMemoryRoomRepository;
import com.framehost.room.service.RoomNotFoundException;

class UploadSessionServiceTest {

    @TempDir
    Path tempDir;

    private InMemoryRoomRepository roomRepository;
    private DeviceAuthenticationService authenticationService;
    private FileSystemUploadStore uploadStore;
    private MediaStorageProperties storageProperties;
    private UploadSessionService uploadSessionService;

    private Room room;
    private Device host;
    private Device follower;

    private static final String ROOM_CODE = "ABC123";
    private static final String HOST_TOKEN = "Bearer host-token";
    private static final String FOLLOWER_TOKEN = "Bearer follower-token";

    // Initialize fresh test dependencies before every test.
    @BeforeEach
    public void setUp() {

        roomRepository = mock(InMemoryRoomRepository.class);
        authenticationService = mock(DeviceAuthenticationService.class);

        // Use temporary storage instead of the real media directory.
        storageProperties = new MediaStorageProperties();
        storageProperties.setStorageDir(tempDir);

        uploadStore = new FileSystemUploadStore(storageProperties);

        // Create an active room.
        room = new Room(ROOM_CODE, Instant.now());

        // Create the Host device.
        host = new Device(
                "host-id",
                "Host Device",
                DeviceRole.HOST,
                Instant.now()
        );

        // Create a Follower device.
        follower = new Device(
                "follower-id",
                "Follower Device",
                DeviceRole.FOLLOWER,
                Instant.now()
        );

        // Register both devices in the room.
        room.addDevice(host, "hashed-host-token");
        room.addDevice(follower, "hashed-follower-token");

        // Configure the mocked repository.
        when(roomRepository.findByRoomCode(ROOM_CODE))
                .thenReturn(Optional.of(room));

        // Initialize the service.
        uploadSessionService = new UploadSessionService(
                roomRepository,
                authenticationService,
                uploadStore,
                storageProperties
        );
    }

    // ==========================================
    // Existing upload-session tests
    // ==========================================
    @Test
    void shouldCreateUploadSessionSuccessfully() {

        when(authenticationService.authenticate(room, HOST_TOKEN))
                .thenReturn(host);

        CreateUploadSessionResponse response
                = uploadSessionService.createUploadSession(
                        ROOM_CODE,
                        createRequest(),
                        HOST_TOKEN
                );

        assertNotNull(response);
        assertEquals(ROOM_CODE, response.roomCode());
        assertEquals("sample.mp4", response.fileName());
        assertEquals("video/mp4", response.mimeType());
        assertEquals(10_485_760L, response.fileSizeBytes());
        assertEquals(3, response.totalChunks());

        assertNotNull(response.uploadId());
        assertNotNull(response.createdAt());
        assertNotNull(response.expiresAt());

        Optional<UploadManifest> savedManifest
                = uploadStore.read(response.uploadId());

        assertTrue(savedManifest.isPresent());
        assertEquals(
                UploadStatus.INITIATED,
                savedManifest.get().status()
        );

        verify(authenticationService).authenticate(room, HOST_TOKEN);
    }

    @Test
    void shouldRejectFollowerUpload() throws IOException {

        when(authenticationService.authenticate(room, FOLLOWER_TOKEN))
                .thenReturn(follower);

        AuthorizationException exception = assertThrows(
                AuthorizationException.class,
                () -> uploadSessionService.createUploadSession(
                        ROOM_CODE,
                        createRequest(),
                        FOLLOWER_TOKEN
                )
        );

        assertNotNull(exception);
        assertEquals(
                "Only the Host can upload local media.",
                exception.getMessage()
        );

        assertNoUploadData();
    }

    @Test
    void shouldRejectMissingAuthentication() throws IOException {

        when(authenticationService.authenticate(room, null))
                .thenThrow(new AuthenticationException());

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> uploadSessionService.createUploadSession(
                        ROOM_CODE,
                        createRequest(),
                        null
                )
        );

        assertNotNull(exception);
        assertNoUploadData();
    }

    @Test
    void shouldRejectInvalidAuthentication() throws IOException {

        when(authenticationService.authenticate(room, HOST_TOKEN))
                .thenThrow(new AuthenticationException());

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> uploadSessionService.createUploadSession(
                        ROOM_CODE,
                        createRequest(),
                        HOST_TOKEN
                )
        );

        assertNotNull(exception);
        assertNoUploadData();
    }

    @Test
    void shouldRejectUploadWhenRoomDoesNotExist() throws IOException {

        when(roomRepository.findByRoomCode("UNKNOWN"))
                .thenReturn(Optional.empty());

        RoomNotFoundException exception = assertThrows(
                RoomNotFoundException.class,
                () -> uploadSessionService.createUploadSession(
                        "UNKNOWN",
                        createRequest(),
                        HOST_TOKEN
                )
        );

        assertNotNull(exception);

        verifyNoInteractions(authenticationService);

        assertNoUploadData();
    }

    @Test
    void shouldRejectFileLargerThanConfiguredLimit() throws IOException {

        storageProperties.setMaxFileSizeBytes(5 * 1024 * 1024L);

        when(authenticationService.authenticate(room, HOST_TOKEN))
                .thenReturn(host);

        CreateUploadSessionRequest request
                = new CreateUploadSessionRequest(
                        "large-video.mp4",
                        "video/mp4",
                        10 * 1024 * 1024L
                );

        UploadLimitExceededException exception = assertThrows(
                UploadLimitExceededException.class,
                () -> uploadSessionService.createUploadSession(
                        ROOM_CODE,
                        request,
                        HOST_TOKEN
                )
        );

        assertNotNull(exception);
        assertNoUploadData();
    }

    @Test
    void shouldNotReplaceExistingMediaSessionWhenUploadStarts() {

        when(authenticationService.authenticate(room, HOST_TOKEN))
                .thenReturn(host);

        MediaSession existingSession = room.createMediaSession(
                host.deviceId(),
                "existing-video.mp4",
                "video/mp4",
                5_000_000L
        );

        uploadSessionService.createUploadSession(
                ROOM_CODE,
                createRequest(),
                HOST_TOKEN
        );

        assertSame(existingSession, room.mediaSession());
    }

    // ==========================================
    // New chunk-upload tests
    // ==========================================
    @Test
    void shouldUploadChunkSuccessfully() throws IOException {

        CreateUploadSessionResponse session = createSmallUploadSession();

        byte[] chunk = {1, 2, 3, 4};

        UploadChunkResponse response
                = uploadSessionService.uploadChunk(
                        ROOM_CODE,
                        session.uploadId(),
                        0,
                        chunk.length,
                        new ByteArrayInputStream(chunk),
                        HOST_TOKEN
                );

        assertEquals(session.uploadId(), response.uploadId());
        assertEquals(0, response.chunkIndex());
        assertEquals(1, response.uploadedChunkCount());
        assertEquals(3, response.totalChunks());
        assertEquals(4L, response.contiguousUploadedBytes());
        assertEquals(UploadStatus.UPLOADING, response.status());

        UploadManifest manifest = uploadStore.read(session.uploadId())
                .orElseThrow();

        assertEquals(UploadStatus.UPLOADING, manifest.status());
        assertTrue(manifest.uploadedChunkIndexes().contains(0));
        assertTrue(uploadStore.chunkExists(session.uploadId(), 0));
    }

    @Test
    void shouldRejectChunkWithIncorrectSize() {

        CreateUploadSessionResponse session = createSmallUploadSession();

        byte[] chunk = {1, 2, 3};

        InvalidUploadChunkException exception = assertThrows(
                InvalidUploadChunkException.class,
                () -> uploadSessionService.uploadChunk(
                        ROOM_CODE,
                        session.uploadId(),
                        0,
                        chunk.length,
                        new ByteArrayInputStream(chunk),
                        HOST_TOKEN
                )
        );

        assertNotNull(exception);
        assertEquals(
                false,
                uploadStore.chunkExists(session.uploadId(), 0)
        );
    }

    @Test
    void shouldRejectInvalidChunkIndex() {

        CreateUploadSessionResponse session = createSmallUploadSession();

        InvalidUploadChunkException exception = assertThrows(
                InvalidUploadChunkException.class,
                () -> uploadSessionService.uploadChunk(
                        ROOM_CODE,
                        session.uploadId(),
                        3,
                        2,
                        new ByteArrayInputStream(new byte[]{1, 2}),
                        HOST_TOKEN
                )
        );

        assertNotNull(exception);
    }

    @Test
    void shouldRejectDuplicateChunkUpload() throws IOException {

        CreateUploadSessionResponse session = createSmallUploadSession();

        byte[] chunk = {1, 2, 3, 4};

        uploadSessionService.uploadChunk(
                ROOM_CODE,
                session.uploadId(),
                0,
                chunk.length,
                new ByteArrayInputStream(chunk),
                HOST_TOKEN
        );

        UploadConflictException exception = assertThrows(
                UploadConflictException.class,
                () -> uploadSessionService.uploadChunk(
                        ROOM_CODE,
                        session.uploadId(),
                        0,
                        chunk.length,
                        new ByteArrayInputStream(chunk),
                        HOST_TOKEN
                )
        );

        assertNotNull(exception);
        assertTrue(uploadStore.chunkExists(session.uploadId(), 0));
    }

    @Test
    void shouldRejectChunkFromDifferentHost() {

        CreateUploadSessionResponse session = createSmallUploadSession();

        Device anotherHost = new Device(
                "another-host-id",
                "Another Host",
                DeviceRole.HOST,
                Instant.now()
        );

        String anotherHostToken = "Bearer another-host-token";

        when(authenticationService.authenticate(room, anotherHostToken))
                .thenReturn(anotherHost);

        AuthorizationException exception = assertThrows(
                AuthorizationException.class,
                () -> uploadSessionService.uploadChunk(
                        ROOM_CODE,
                        session.uploadId(),
                        0,
                        4,
                        new ByteArrayInputStream(new byte[]{1, 2, 3, 4}),
                        anotherHostToken
                )
        );

        assertNotNull(exception);
        assertEquals(
                false,
                uploadStore.chunkExists(session.uploadId(), 0)
        );
    }

    @Test
    void shouldCalculateProgressForOutOfOrderChunks() throws IOException {

        CreateUploadSessionResponse session = createSmallUploadSession();

        // Upload chunk 1 before chunk 0.
        UploadChunkResponse secondChunk
                = uploadSessionService.uploadChunk(
                        ROOM_CODE,
                        session.uploadId(),
                        1,
                        4,
                        new ByteArrayInputStream(new byte[]{5, 6, 7, 8}),
                        HOST_TOKEN
                );

        assertEquals(1, secondChunk.uploadedChunkCount());
        assertEquals(0L, secondChunk.contiguousUploadedBytes());

        // Upload chunk 0.
        UploadChunkResponse firstChunk
                = uploadSessionService.uploadChunk(
                        ROOM_CODE,
                        session.uploadId(),
                        0,
                        4,
                        new ByteArrayInputStream(new byte[]{1, 2, 3, 4}),
                        HOST_TOKEN
                );

        assertEquals(2, firstChunk.uploadedChunkCount());
        assertEquals(8L, firstChunk.contiguousUploadedBytes());

        // Upload the final 2-byte chunk.
        UploadChunkResponse finalChunk
                = uploadSessionService.uploadChunk(
                        ROOM_CODE,
                        session.uploadId(),
                        2,
                        2,
                        new ByteArrayInputStream(new byte[]{9, 10}),
                        HOST_TOKEN
                );

        assertEquals(3, finalChunk.uploadedChunkCount());
        assertEquals(10L, finalChunk.contiguousUploadedBytes());

        UploadManifest manifest = uploadStore.read(session.uploadId())
                .orElseThrow();

        assertEquals(3, manifest.uploadedChunkIndexes().size());
        assertEquals(10L, manifest.contiguousUploadedBytes());

        // All chunks exist, but assembly and verification are still pending.
        assertEquals(UploadStatus.UPLOADING, manifest.status());
    }

    @Test
    void shouldRollbackChunkWhenManifestSaveFails() {

        // Spy on the real filesystem store.
        uploadStore = spy(uploadStore);

        uploadSessionService = new UploadSessionService(
                roomRepository,
                authenticationService,
                uploadStore,
                storageProperties
        );

        CreateUploadSessionResponse session = createSmallUploadSession();

        // Fail only when the updated UPLOADING manifest is saved.
        doThrow(new UploadStorageException(
                "Simulated manifest save failure.",
                new IOException("Simulated storage failure.")
        )).when(uploadStore).save(
                argThat(manifest
                        -> manifest != null
                        && manifest.status() == UploadStatus.UPLOADING
                )
        );

        UploadStorageException exception = assertThrows(
                UploadStorageException.class,
                () -> uploadSessionService.uploadChunk(
                        ROOM_CODE,
                        session.uploadId(),
                        0,
                        4,
                        new ByteArrayInputStream(new byte[]{1, 2, 3, 4}),
                        HOST_TOKEN
                )
        );

        assertNotNull(exception);

        // The newly written chunk must be removed.
        assertEquals(
                false,
                uploadStore.chunkExists(session.uploadId(), 0)
        );

        // The original manifest must remain unchanged.
        UploadManifest manifest = uploadStore.read(session.uploadId())
                .orElseThrow();

        assertEquals(UploadStatus.INITIATED, manifest.status());
        assertTrue(manifest.uploadedChunkIndexes().isEmpty());
        assertEquals(0L, manifest.contiguousUploadedBytes());
    }

    // ==========================================
    // Test helpers
    // ==========================================
    private CreateUploadSessionRequest createRequest() {
        return new CreateUploadSessionRequest(
                "sample.mp4",
                "video/mp4",
                10_485_760L
        );
    }

    // Creates a small 10-byte upload with 4-byte chunks.
    private CreateUploadSessionResponse createSmallUploadSession() {

        storageProperties.setChunkSizeBytes(4);

        when(authenticationService.authenticate(room, HOST_TOKEN))
                .thenReturn(host);

        CreateUploadSessionRequest request
                = new CreateUploadSessionRequest(
                        "small-video.mp4",
                        "video/mp4",
                        10L
                );

        return uploadSessionService.createUploadSession(
                ROOM_CODE,
                request,
                HOST_TOKEN
        );
    }

    // Verifies that rejected requests did not create upload files.
    private void assertNoUploadData() throws IOException {
        try (var files = Files.list(tempDir)) {
            assertTrue(
                    files.findAny().isEmpty(),
                    "No upload data should have been created."
            );
        }
    }
}
