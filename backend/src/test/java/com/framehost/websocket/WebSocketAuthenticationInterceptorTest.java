package com.framehost.websocket;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import com.framehost.common.AuthenticationException;
import com.framehost.device.Device;
import com.framehost.device.DeviceRole;
import com.framehost.device.service.DeviceAuthenticationService;
import com.framehost.room.model.Room;
import com.framehost.room.repository.InMemoryRoomRepository;

class WebSocketAuthenticationInterceptorTest {

    private InMemoryRoomRepository roomRepository;
    private DeviceAuthenticationService authenticationService;
    private WebSocketAuthenticationInterceptor interceptor;

    private Room room;
    private Device device;

    @BeforeEach
    public void setUp() {

        roomRepository = mock(InMemoryRoomRepository.class);
        authenticationService = mock(DeviceAuthenticationService.class);

        interceptor = new WebSocketAuthenticationInterceptor(
                roomRepository,
                authenticationService
        );

        room = mock(Room.class);
        device = mock(Device.class);

        when(room.roomCode()).thenReturn("ABCD12");

        when(device.deviceId()).thenReturn("device-123");
        when(device.role()).thenReturn(DeviceRole.HOST);

        when(roomRepository.findByRoomCode("ABCD12"))
                .thenReturn(Optional.of(room));

        when(authenticationService.authenticate(
                room,
                "Bearer valid-token"
        )).thenReturn(device);
    }

    // CONNECT authentication tests
    @Test
    void shouldAuthenticateValidHostConnection() {

        Message<byte[]> message = createConnectMessage(
                "ABCD12",
                "Bearer valid-token"
        );

        Message<?> result = interceptor.preSend(message, null);

        assertSame(message, result);

        StompHeaderAccessor accessor
                = StompHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class
                );

        AuthenticatedDevicePrincipal principal
                = (AuthenticatedDevicePrincipal) accessor.getUser();

        assertEquals("device-123", principal.getName());
        assertEquals("device-123", principal.deviceId());
        assertEquals("ABCD12", principal.roomCode());
        assertEquals(DeviceRole.HOST, principal.role());

        verify(authenticationService).authenticate(
                room,
                "Bearer valid-token"
        );
    }

    @Test
    void shouldRejectConnectionWithoutRoomCode() {

        Message<byte[]> message = createConnectMessage(
                null,
                "Bearer valid-token"
        );

        assertThrowsExpected(
                AuthenticationException.class,
                () -> interceptor.preSend(message, null)
        );
    }

    @Test
    void shouldRejectConnectionWithInvalidToken() {

        when(authenticationService.authenticate(
                room,
                "Bearer invalid-token"
        )).thenThrow(new AuthenticationException());

        Message<byte[]> message = createConnectMessage(
                "ABCD12",
                "Bearer invalid-token"
        );

        assertThrowsExpected(
                AuthenticationException.class,
                () -> interceptor.preSend(message, null)
        );
    }

    @Test
    void shouldRejectConnectionForUnknownRoom() {

        when(roomRepository.findByRoomCode("UNKNOWN"))
                .thenReturn(Optional.empty());

        Message<byte[]> message = createConnectMessage(
                "UNKNOWN",
                "Bearer valid-token"
        );

        assertThrowsExpected(
                AuthenticationException.class,
                () -> interceptor.preSend(message, null)
        );
    }

    // SUBSCRIBE authorization tests
    @Test
    void shouldAllowSubscriptionToOwnRoomPlaybackTopic() {

        AuthenticatedDevicePrincipal principal
                = createPrincipal(DeviceRole.HOST);

        Message<byte[]> message = createSubscribeMessage(
                principal,
                "/topic/rooms/ABCD12/playback"
        );

        Message<?> result = interceptor.preSend(message, null);

        assertSame(message, result);
    }

    @Test
    void shouldRejectSubscriptionToAnotherRoom() {

        AuthenticatedDevicePrincipal principal
                = createPrincipal(DeviceRole.HOST);

        Message<byte[]> message = createSubscribeMessage(
                principal,
                "/topic/rooms/OTHER12/playback"
        );

        assertThrowsExpected(
                MessagingException.class,
                () -> interceptor.preSend(message, null)
        );
    }

    @Test
    void shouldRejectSubscriptionWithoutAuthenticatedPrincipal() {

        Message<byte[]> message = createSubscribeMessage(
                null,
                "/topic/rooms/ABCD12/playback"
        );

        assertThrowsExpected(
                AuthenticationException.class,
                () -> interceptor.preSend(message, null)
        );
    }

    // SEND authorization tests
    @Test
    void shouldAllowAuthenticatedDeviceToSendToOwnRoomPlaybackEndpoint() {

        AuthenticatedDevicePrincipal principal
                = createPrincipal(DeviceRole.HOST);

        Message<byte[]> message = createSendMessage(
                principal,
                "/app/rooms/ABCD12/playback"
        );

        Message<?> result = interceptor.preSend(message, null);

        assertSame(message, result);
    }

    @Test
    void shouldRejectSendToAnotherRoomPlaybackEndpoint() {

        AuthenticatedDevicePrincipal principal
                = createPrincipal(DeviceRole.HOST);

        Message<byte[]> message = createSendMessage(
                principal,
                "/app/rooms/OTHER12/playback"
        );

        assertThrowsExpected(
                MessagingException.class,
                () -> interceptor.preSend(message, null)
        );
    }

    @Test
    void shouldRejectSendWithoutAuthenticatedPrincipal() {

        Message<byte[]> message = createSendMessage(
                null,
                "/app/rooms/ABCD12/playback"
        );

        assertThrowsExpected(
                AuthenticationException.class,
                () -> interceptor.preSend(message, null)
        );
    }

    // Reusable assertion helper
    private <T extends Throwable> void assertThrowsExpected(
            Class<T> expectedType,
            Executable executable
    ) {

        T exception = assertThrows(expectedType, executable);

        assertTrue(
                expectedType.isInstance(exception),
                "Unexpected exception type"
        );
    }

    // Helper methods
    private AuthenticatedDevicePrincipal createPrincipal(DeviceRole role) {

        return new AuthenticatedDevicePrincipal(
                "device-123",
                "ABCD12",
                "device-123",
                role
        );
    }

    private Message<byte[]> createConnectMessage(
            String roomCode,
            String authorization
    ) {

        StompHeaderAccessor accessor
                = StompHeaderAccessor.create(StompCommand.CONNECT);

        if (roomCode != null) {
            accessor.setNativeHeader("roomCode", roomCode);
        }

        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }

        return createMessage(accessor);
    }

    private Message<byte[]> createSubscribeMessage(
            AuthenticatedDevicePrincipal principal,
            String destination
    ) {

        StompHeaderAccessor accessor
                = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);

        accessor.setDestination(destination);

        if (principal != null) {
            accessor.setUser(principal);
        }

        return createMessage(accessor);
    }

    private Message<byte[]> createSendMessage(
            AuthenticatedDevicePrincipal principal,
            String destination
    ) {

        StompHeaderAccessor accessor
                = StompHeaderAccessor.create(StompCommand.SEND);

        accessor.setDestination(destination);

        if (principal != null) {
            accessor.setUser(principal);
        }

        return createMessage(accessor);
    }

    private Message<byte[]> createMessage(StompHeaderAccessor accessor) {

        accessor.setLeaveMutable(true);

        return MessageBuilder.createMessage(
                new byte[0],
                accessor.getMessageHeaders()
        );
    }
}
