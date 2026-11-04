package com.framehost.room;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.framehost.analytics.service.AnalyticsService;
import com.framehost.common.AuthenticationException;
import com.framehost.common.AuthorizationException;
import com.framehost.device.Device;
import com.framehost.device.DeviceRole;
import com.framehost.device.dto.DeviceManagementResponse;
import com.framehost.device.dto.DeviceResponse;
import com.framehost.device.service.DeviceAuthenticationService;
import com.framehost.device.service.DeviceNotFoundException;
import com.framehost.device.service.DeviceTokenService;
import com.framehost.room.model.Room;
import com.framehost.room.repository.InMemoryRoomRepository;
import com.framehost.room.service.RoomCodeGenerator;
import com.framehost.room.service.RoomOperationConflictException;
import com.framehost.room.service.RoomService;

class RoomServiceDeviceManagementTest {

    private static final String ROOM_CODE = "ABC123";
    private static final String HOST_TOKEN = "test-host-token";
    private static final String FOLLOWER_TOKEN = "test-follower-token";
    private static final String INVALID_TOKEN = "invalid-token";

    private InMemoryRoomRepository repository;
    private RoomService roomService;
    private DeviceTokenService tokenService;
    private Room room;

    private Device host;
    private Device follower;

    private String hostAuthorization;
    private String followerAuthorization;

    // JUnit invokes this method automatically before every test.
    @BeforeEach
    public void initializeTestData() {

        repository = mock(InMemoryRoomRepository.class);

        tokenService = new DeviceTokenService();

        DeviceAuthenticationService authenticationService
                = new DeviceAuthenticationService(tokenService);

        room = new Room(ROOM_CODE, Instant.now());

        host = new Device(
                UUID.randomUUID().toString(),
                "Host",
                DeviceRole.HOST,
                Instant.now()
        );

        follower = new Device(
                UUID.randomUUID().toString(),
                "Laptop",
                DeviceRole.FOLLOWER,
                Instant.now()
        );

        room.addDevice(host, tokenService.hashToken(HOST_TOKEN));
        room.addDevice(follower, tokenService.hashToken(FOLLOWER_TOKEN));

        when(repository.findByRoomCode(ROOM_CODE))
                .thenReturn(Optional.of(room));

        roomService = new RoomService(
                repository,
                mock(RoomCodeGenerator.class),
                tokenService,
                authenticationService,
                mock(AnalyticsService.class)
        );

        hostAuthorization = "Bearer " + HOST_TOKEN;
        followerAuthorization = "Bearer " + FOLLOWER_TOKEN;
    }

    // Reusable helper: verifies that the expected exception is thrown.
    private static <T extends Throwable> void assertThrowsAndVerify(
            Class<T> expectedType,
            Executable executable
    ) {
        T exception = assertThrows(expectedType, executable);
        assertNotNull(exception);
    }

    @Test
    void shouldAllowHostToViewDeviceList() {

        List<DeviceResponse> devices = roomService.getDevices(
                ROOM_CODE,
                hostAuthorization
        );

        assertEquals(2, devices.size());
        assertEquals(2, room.deviceCount());
    }

    @Test
    void shouldRejectFollowerWhenViewingDeviceList() {

        assertThrowsAndVerify(
                AuthorizationException.class,
                () -> roomService.getDevices(
                        ROOM_CODE,
                        followerAuthorization
                )
        );
    }

    @Test
    void shouldRejectMissingAuthenticationToken() {

        assertThrowsAndVerify(
                AuthenticationException.class,
                () -> roomService.getDevices(ROOM_CODE, null)
        );
    }

    @Test
    void shouldRejectInvalidAuthenticationToken() {

        assertThrowsAndVerify(
                AuthenticationException.class,
                () -> roomService.getDevices(
                        ROOM_CODE,
                        "Bearer " + INVALID_TOKEN
                )
        );
    }

    @Test
    void shouldAllowFollowerToLeaveRoom() {

        DeviceManagementResponse response = roomService.leaveRoom(
                ROOM_CODE,
                followerAuthorization
        );

        assertEquals(follower.deviceId(), response.deviceId());
        assertEquals(1, response.deviceCount());
        assertEquals(1, room.deviceCount());
    }

    @Test
    void shouldRejectHostLeavingActiveRoom() {

        assertThrowsAndVerify(
                RoomOperationConflictException.class,
                () -> roomService.leaveRoom(
                        ROOM_CODE,
                        hostAuthorization
                )
        );

        assertEquals(2, room.deviceCount());
    }

    @Test
    void shouldAllowHostToRemoveFollower() {

        DeviceManagementResponse response = roomService.removeDevice(
                ROOM_CODE,
                follower.deviceId(),
                hostAuthorization
        );

        assertEquals(follower.deviceId(), response.deviceId());
        assertEquals(1, response.deviceCount());
        assertEquals(1, room.deviceCount());
    }

    @Test
    void shouldRejectFollowerRemovingAnotherDevice() {

        assertThrowsAndVerify(
                AuthorizationException.class,
                () -> roomService.removeDevice(
                        ROOM_CODE,
                        host.deviceId(),
                        followerAuthorization
                )
        );

        assertEquals(2, room.deviceCount());
    }

    @Test
    void shouldRejectHostRemovingItself() {

        assertThrowsAndVerify(
                RoomOperationConflictException.class,
                () -> roomService.removeDevice(
                        ROOM_CODE,
                        host.deviceId(),
                        hostAuthorization
                )
        );

        assertEquals(2, room.deviceCount());
    }

    @Test
    void shouldReturnNotFoundWhenTargetDeviceDoesNotExist() {

        assertThrowsAndVerify(
                DeviceNotFoundException.class,
                () -> roomService.removeDevice(
                        ROOM_CODE,
                        "unknown-device",
                        hostAuthorization
                )
        );
    }

    @Test
    void shouldRevokeTokenAfterFollowerLeaves() {

        roomService.leaveRoom(ROOM_CODE, followerAuthorization);

        assertThrowsAndVerify(
                AuthenticationException.class,
                () -> roomService.getDevices(
                        ROOM_CODE,
                        followerAuthorization
                )
        );
    }

    @Test
    void shouldRevokeTokenAfterHostRemovesFollower() {

        roomService.removeDevice(
                ROOM_CODE,
                follower.deviceId(),
                hostAuthorization
        );

        assertThrowsAndVerify(
                AuthenticationException.class,
                () -> roomService.leaveRoom(
                        ROOM_CODE,
                        followerAuthorization
                )
        );
    }
}
