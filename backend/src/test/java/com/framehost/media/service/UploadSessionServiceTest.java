package com.framehost.media.service;

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
import static org.mockito.Mockito.mock;
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

    // JUnit invokes this method automatically before each test.
    @BeforeEach
    public void setUp() {

        // Create fresh mocks for each test.
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

    @Test
    void shouldCreateUploadSessionSuccessfully() {

        // Authenticate the Host.
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

        // 10 MiB divided into 4 MiB chunks requires 3 chunks.
        assertEquals(3, response.totalChunks());

        assertNotNull(response.uploadId());
        assertNotNull(response.createdAt());
        assertNotNull(response.expiresAt());

        // Verify that the manifest was persisted.
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

        // Authentication should not happen for a missing room.
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

        // Create an existing active media session.
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

        // Starting an upload must not replace active playback media.
        assertSame(existingSession, room.mediaSession());
    }

    private CreateUploadSessionRequest createRequest() {
        return new CreateUploadSessionRequest(
                "sample.mp4",
                "video/mp4",
                10_485_760L
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
