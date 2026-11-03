package com.framehost.playback.controller;

import java.security.Principal;
import java.util.Locale;

import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import com.framehost.device.Device;
import com.framehost.playback.dto.PlaybackCommandMessage;
import com.framehost.playback.dto.PlaybackStateMessage;
import com.framehost.playback.model.PlaybackState;
import com.framehost.playback.service.PlaybackCommandService;
import com.framehost.room.model.Room;
import com.framehost.room.repository.InMemoryRoomRepository;
import com.framehost.websocket.AuthenticatedDevicePrincipal;

import jakarta.validation.Valid;

// Handles authenticated playback commands and broadcasts room state.
@Controller
@Validated
public class PlaybackCommandController {

    private static final String PLAYBACK_TOPIC_PREFIX = "/topic/rooms/";
    private static final String PLAYBACK_TOPIC_SUFFIX = "/playback";

    private final InMemoryRoomRepository roomRepository;
    private final PlaybackCommandService playbackCommandService;
    private final SimpMessagingTemplate messagingTemplate;

    public PlaybackCommandController(
            InMemoryRoomRepository roomRepository,
            PlaybackCommandService playbackCommandService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.roomRepository = roomRepository;
        this.playbackCommandService = playbackCommandService;
        this.messagingTemplate = messagingTemplate;
    }

    // Handles /app/rooms/{roomCode}/playback messages.
    @MessageMapping("/rooms/{roomCode}/playback")
    public void handlePlaybackCommand(
            @DestinationVariable String roomCode,
            @Payload @Valid PlaybackCommandMessage command,
            Principal user
    ) {

        // Only authenticated WebSocket devices are trusted.
        if (!(user instanceof AuthenticatedDevicePrincipal principal)) {
            throw new MessagingException("Authentication required.");
        }

        String normalizedRoomCode = roomCode
                .trim()
                .toUpperCase(Locale.ROOT);

        // Ensure the destination matches the authenticated room.
        if (!principal.roomCode().equals(normalizedRoomCode)) {
            throw new MessagingException("Room access denied.");
        }

        Room room = roomRepository.findByRoomCode(normalizedRoomCode)
                .orElseThrow(
                        () -> new MessagingException("Room not found.")
                );

        // Serialize device validation, execution, and broadcasting.
        synchronized (room) {

            if (!room.isActive()) {
                throw new MessagingException("Room is inactive.");
            }

            // Verify that the device still belongs to the room.
            Device currentDevice = room.findDevice(principal.deviceId());

            if (currentDevice == null) {
                throw new MessagingException(
                        "Device is no longer a member of this room."
                );
            }

            // Verify that the current role matches the authenticated identity.
            if (currentDevice.role() != principal.role()) {
                throw new MessagingException(
                        "Device authorization is no longer valid."
                );
            }

            try {
                // The service checks Host privileges and Follower permissions.
                PlaybackState updatedState
                        = playbackCommandService.execute(
                                room,
                                principal.deviceId(),
                                command
                        );

                // Broadcast only the authoritative server-generated state.
                PlaybackStateMessage message
                        = PlaybackStateMessage.from(updatedState);

                messagingTemplate.convertAndSend(
                        PLAYBACK_TOPIC_PREFIX
                        + normalizedRoomCode
                        + PLAYBACK_TOPIC_SUFFIX,
                        message
                );

            } catch (IllegalArgumentException | IllegalStateException exception) {
                throw new MessagingException(
                        exception.getMessage(),
                        exception
                );
            }
        }
    }
}
