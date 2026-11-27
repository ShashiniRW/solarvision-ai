package com.solarvision.backend.user;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Creates one-time passwords for new accounts and password resets.
 * SecureRandom (not Random) so the passwords cannot be predicted.
 * Look-alike characters (0/O, 1/l/I) are left out so they are easy to read out and type.
 */
@Component
public class TemporaryPasswordGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int LENGTH = 16;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder password = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            password.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return password.toString();
    }
}
