package com.framehost.media.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.framehost.media.dto.CreateUploadSessionRequest;
import com.framehost.media.dto.CreateUploadSessionResponse;
import com.framehost.media.service.UploadSessionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/rooms/{roomCode}/uploads")
public class UploadSessionController {

    private final UploadSessionService uploadSessionService;

    public UploadSessionController(
            UploadSessionService uploadSessionService
    ) {
        this.uploadSessionService = uploadSessionService;
    }

    // Create a new upload session for the Host.
    @PostMapping
    public ResponseEntity<CreateUploadSessionResponse> createUploadSession(
            @PathVariable String roomCode,
            @Valid @RequestBody CreateUploadSessionRequest request,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorizationHeader
    ) {

        CreateUploadSessionResponse response
                = uploadSessionService.createUploadSession(
                        roomCode,
                        request,
                        authorizationHeader
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
