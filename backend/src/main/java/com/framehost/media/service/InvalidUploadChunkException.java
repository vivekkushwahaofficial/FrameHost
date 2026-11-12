package com.framehost.media.service;

public class InvalidUploadChunkException extends RuntimeException {

    public InvalidUploadChunkException(String message) {
        super(message);
    }
}
