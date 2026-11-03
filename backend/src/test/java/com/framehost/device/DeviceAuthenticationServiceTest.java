package com.framehost.device;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.framehost.common.AuthenticationException;
import com.framehost.device.service.DeviceAuthenticationService;
import com.framehost.device.service.DeviceTokenService;
import com.framehost.room.model.Room;

class DeviceAuthenticationServiceTest {

    private DeviceTokenService tokenService;
    private DeviceAuthenticationService authenticationService;

    private Room room;
    private Device host;
    private String hostToken;

    @BeforeEach
    public void setUp() {
        tokenService = new DeviceTokenService();

        authenticationService = new DeviceAuthenticationService(
                tokenService
        );

        hostToken = "test-host-token";

        room = new Room("ABC123", Instant.now());

        host = new Device(
                "host-id",
                "Host Device",
                DeviceRole.HOST,
                Instant.now()
        );

        // Store only the hash of the original authentication token.
        room.addDevice(
                host,
                tokenService.hashToken(hostToken)
        );
    }

    @Test
    void shouldAuthenticateDeviceWithValidToken() {
        Device authenticatedDevice = authenticationService.authenticate(
                room,
                "Bearer " + hostToken
        );

        assertNotNull(authenticatedDevice);
        assertEquals(host.deviceId(), authenticatedDevice.deviceId());
        assertEquals(DeviceRole.HOST, authenticatedDevice.role());
    }

    @Test
    void shouldRejectMissingAuthorizationHeader() {
        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> authenticationService.authenticate(room, null)
        );

        assertNotNull(exception);
    }

    @Test
    void shouldRejectInvalidToken() {
        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> authenticationService.authenticate(
                        room,
                        "Bearer invalid-token"
                )
        );

        assertNotNull(exception);
    }

    @Test
    void shouldRejectTokenAfterRoomHasEnded() {
        // Verify that the token works before room termination.
        assertNotNull(
                authenticationService.authenticate(
                        room,
                        "Bearer " + hostToken
                )
        );

        // Terminate the room and revoke its device tokens.
        room.end();

        // The previously valid token must now be rejected.
        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> authenticationService.authenticate(
                        room,
                        "Bearer " + hostToken
                )
        );

        assertNotNull(exception);
    }
}
