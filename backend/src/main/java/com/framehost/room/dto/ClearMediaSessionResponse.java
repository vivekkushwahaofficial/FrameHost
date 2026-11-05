package com.framehost.room.dto;

// Response returned after clearing a media session.
public record ClearMediaSessionResponse(
        String roomCode,
        String message
        ) {

}
