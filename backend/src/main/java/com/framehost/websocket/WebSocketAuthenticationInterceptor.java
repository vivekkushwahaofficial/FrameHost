package com.framehost.websocket;

import java.util.Locale;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import com.framehost.common.AuthenticationException;
import com.framehost.device.Device;
import com.framehost.device.service.DeviceAuthenticationService;
import com.framehost.room.model.Room;
import com.framehost.room.repository.InMemoryRoomRepository;

// Authenticates WebSocket connections and restricts room messaging.
@Component
public class WebSocketAuthenticationInterceptor implements ChannelInterceptor {

    private static final String PLAYBACK_TOPIC_PREFIX = "/topic/rooms/";
    private static final String PLAYBACK_TOPIC_SUFFIX = "/playback";
    private static final String PLAYBACK_APP_PREFIX = "/app/rooms/";

    private final InMemoryRoomRepository roomRepository;
    private final DeviceAuthenticationService authenticationService;

    public WebSocketAuthenticationInterceptor(
            InMemoryRoomRepository roomRepository,
            DeviceAuthenticationService authenticationService
    ) {
        this.roomRepository = roomRepository;
        this.authenticationService = authenticationService;
    }

    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel
    ) {

        StompHeaderAccessor accessor
                = MessageHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class
                );

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (command == StompCommand.CONNECT) {
            authenticateConnection(accessor);
        }

        if (command == StompCommand.SUBSCRIBE) {
            validateSubscription(accessor);
        }

        if (command == StompCommand.SEND) {
            validateSend(accessor);
        }

        return message;
    }

    // Authenticates the device when establishing a WebSocket connection.
    private void authenticateConnection(
            StompHeaderAccessor accessor
    ) {

        String roomCodeHeader
                = accessor.getFirstNativeHeader("roomCode");

        String authorizationHeader
                = accessor.getFirstNativeHeader("Authorization");

        if (roomCodeHeader == null || roomCodeHeader.isBlank()) {
            throw new AuthenticationException();
        }

        String roomCode = roomCodeHeader
                .trim()
                .toUpperCase(Locale.ROOT);

        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(AuthenticationException::new);

        Device device = authenticationService.authenticate(
                room,
                authorizationHeader
        );

        // Store the authenticated device identity in the WebSocket session.
        accessor.setUser(
                new AuthenticatedDevicePrincipal(
                        device.deviceId(),
                        room.roomCode(),
                        device.deviceId(),
                        device.role()
                )
        );
    }

    // Allows subscriptions only to the authenticated device's room.
    private void validateSubscription(
            StompHeaderAccessor accessor
    ) {

        AuthenticatedDevicePrincipal principal
                = requirePrincipal(accessor);

        String expectedDestination
                = PLAYBACK_TOPIC_PREFIX
                + principal.roomCode()
                + PLAYBACK_TOPIC_SUFFIX;

        if (!expectedDestination.equals(accessor.getDestination())) {
            throw new MessagingException(
                    "You cannot subscribe to this room."
            );
        }
    }

    // Allows SEND messages only to the authenticated room's playback endpoint.
    private void validateSend(
            StompHeaderAccessor accessor
    ) {

        AuthenticatedDevicePrincipal principal
                = requirePrincipal(accessor);

        String expectedDestination
                = PLAYBACK_APP_PREFIX
                + principal.roomCode()
                + PLAYBACK_TOPIC_SUFFIX;

        if (!expectedDestination.equals(accessor.getDestination())) {
            throw new MessagingException(
                    "You cannot send messages to this destination."
            );
        }
    }

    // Requires an authenticated device identity.
    private AuthenticatedDevicePrincipal requirePrincipal(
            StompHeaderAccessor accessor
    ) {

        if (!(accessor.getUser() instanceof AuthenticatedDevicePrincipal principal)) {
            throw new AuthenticationException();
        }

        return principal;
    }
}
