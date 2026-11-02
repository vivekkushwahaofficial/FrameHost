package com.framehost.common;

// Thrown when a device provides invalid authentication credentials.
public class AuthenticationException extends RuntimeException {

    public AuthenticationException() {
        super("Invalid or missing device authentication token.");
    }
}
