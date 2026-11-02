package com.framehost.common;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.framehost.device.service.DeviceNotFoundException;
import com.framehost.room.service.RoomFullException;
import com.framehost.room.service.RoomNotFoundException;
import com.framehost.room.service.RoomOperationConflictException;

// Centralizes API exception handling.
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handles missing or invalid authentication credentials.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(
            AuthenticationException exception
    ) {
        return createErrorResponse(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage()
        );
    }

    // Handles authenticated devices without sufficient permissions.
    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthorization(
            AuthorizationException exception
    ) {
        return createErrorResponse(
                HttpStatus.FORBIDDEN,
                exception.getMessage()
        );
    }

    // Handles requests for rooms that do not exist.
    @ExceptionHandler(RoomNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleRoomNotFound(
            RoomNotFoundException exception
    ) {
        return createErrorResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }

    // Handles requests for devices that do not exist.
    @ExceptionHandler(DeviceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleDeviceNotFound(
            DeviceNotFoundException exception
    ) {
        return createErrorResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }

    // Handles attempts to join full rooms.
    @ExceptionHandler(RoomFullException.class)
    public ResponseEntity<ApiErrorResponse> handleRoomFull(
            RoomFullException exception
    ) {
        return createErrorResponse(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
    }

    // Handles operations that violate room business rules.
    @ExceptionHandler(RoomOperationConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleRoomOperationConflict(
            RoomOperationConflictException exception
    ) {
        return createErrorResponse(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
    }

    // Handles invalid request bodies.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Invalid request");

        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    // Handles room-code generation failures.
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(
            IllegalStateException exception
    ) {
        return createErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getMessage()
        );
    }

    // Handles unexpected application errors.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception exception
    ) {
        return createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred."
        );
    }

    // Creates a consistent API error response.
    private ResponseEntity<ApiErrorResponse> createErrorResponse(
            HttpStatus status,
            String message
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                status.value(),
                message,
                Instant.now()
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }

    // Standard API error structure.
    public record ApiErrorResponse(
            int status,
            String message,
            Instant timestamp
            ) {

    }
}
