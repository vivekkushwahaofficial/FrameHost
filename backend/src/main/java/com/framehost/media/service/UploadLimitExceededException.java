package com.framehost.media.service;

// Thrown when an upload exceeds the configured size limit.
public class UploadLimitExceededException extends RuntimeException {

    public UploadLimitExceededException(String message) {
        super(message);
    }
}
