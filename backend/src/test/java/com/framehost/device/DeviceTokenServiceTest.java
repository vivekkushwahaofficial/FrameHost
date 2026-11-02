package com.framehost.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import com.framehost.device.service.DeviceTokenService;

class DeviceTokenServiceTest {

    private final DeviceTokenService tokenService = new DeviceTokenService();

    @Test
    void shouldGenerateDifferentTokens() {
        // Generate two independent tokens.
        String firstToken = tokenService.generateToken();
        String secondToken = tokenService.generateToken();

        // Both tokens should exist and be different.
        assertNotNull(firstToken);
        assertNotNull(secondToken);
        assertNotEquals(firstToken, secondToken);
    }

    @Test
    void shouldGenerateUrlSafeToken() {
        // Generate an authentication token.
        String token = tokenService.generateToken();

        // A 32-byte token encoded using Base64 URL encoding is 43 characters.
        assertEquals(43, token.length());
        assertEquals(true, token.matches("[A-Za-z0-9_-]+"));
    }

    @Test
    void shouldGenerateConsistentTokenHash() {
        // Hash the same token twice.
        String token = "sample-device-token";

        String firstHash = tokenService.hashToken(token);
        String secondHash = tokenService.hashToken(token);

        // SHA-256 should produce the same hash for the same input.
        assertEquals(firstHash, secondHash);
        assertNotEquals(token, firstHash);
    }
}
