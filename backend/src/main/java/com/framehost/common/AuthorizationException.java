package com.framehost.common;

// Thrown when an authenticated device lacks required permissions.
public class AuthorizationException extends RuntimeException {

    public AuthorizationException(String message) {
        super(message);
    }
}
