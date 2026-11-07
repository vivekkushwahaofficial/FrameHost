package com.framehost.media.service;

// Represents an error while reading or writing upload data.
public class UploadStorageException extends RuntimeException {

    public UploadStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
