package ru.chessinsight.infrastructure.security.service.impl;

import org.junit.jupiter.api.Test;
import io.qameta.allure.Tag;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class BCryptPasswordHasherTest {
    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    void hash_returnsDifferentFromRaw() {
        String hash = hasher.hash("secret");

        assertNotNull(hash);
        assertNotEquals("secret", hash);
    }

    @Test
    void hash_returnsDifferentForSameInput() {
        String h1 = hasher.hash("secret");
        String h2 = hasher.hash("secret");

        assertNotEquals(h1, h2);
    }

    @Test
    void verify_returnsTrue_whenMatches() {
        String hash = hasher.hash("secret");

        assertTrue(hasher.verify("secret", hash));
    }

    @Test
    void verify_returnsFalse_whenNotMatch() {
        String hash = hasher.hash("secret");

        assertFalse(hasher.verify("wrong", hash));
    }
}