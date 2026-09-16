package com.academic.planning.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    private final PasswordHasher passwordHasher = new PasswordHasher();

    @Test
    void shouldVerifyStoredDemoPassword() {
        String stored = "pbkdf2_sha256$120000$2IS1AXELLN4mCwn9TmRCgA==$ahceD1SndLoDTO6DCJc/ELoqocUk/6EQ1TEWjyr5Ww0=";

        assertTrue(passwordHasher.matches("Admin@123", stored));
        assertFalse(passwordHasher.matches("wrong-password", stored));
    }

    @Test
    void generatedHashShouldRoundTrip() {
        String encoded = passwordHasher.hash("StrongPassword123!");

        assertTrue(passwordHasher.matches("StrongPassword123!", encoded));
        assertFalse(passwordHasher.matches("StrongPassword123", encoded));
    }
}
