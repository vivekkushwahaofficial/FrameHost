
package com.framehost.room.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

// Generates unpredictable room codes.
@Component
public class RoomCodeGenerator {

    // Excludes confusing characters such as I, O, 0 and 1.
    private static final String CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    // Every room code contains six characters.
    private static final int CODE_LENGTH = 6;

    // SecureRandom provides cryptographically strong random values.
    private final SecureRandom secureRandom = new SecureRandom();

    // Generates a new six-character room code.
    public String generate() {

        // Creates a builder for the generated code.
        StringBuilder code = new StringBuilder(CODE_LENGTH);

        // Selects one random character at a time.
        for (int i = 0; i < CODE_LENGTH; i++) {

            // Generates a valid random character index.
            int index = secureRandom.nextInt(CHARACTERS.length());

            // Adds the selected character.
            code.append(CHARACTERS.charAt(index));
        }

        // Returns the completed room code.
        return code.toString();
    }
}
