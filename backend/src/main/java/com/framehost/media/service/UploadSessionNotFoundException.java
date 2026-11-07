package com.framehost.media.service;

// Thrown when an upload session cannot be found.
public class UploadSessionNotFoundException extends RuntimeException {

    public UploadSessionNotFoundException(String message) {
        super(message);
    }
}
