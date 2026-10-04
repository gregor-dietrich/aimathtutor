package de.vptr.aimathtutor.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.service.security.PasswordHashingService;

class PasswordUtilTest {

    private static ByteArrayInputStream stdin(final String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("A password containing a space hashes as a whole, not as its first word")
    void passwordWithSpaceHashesWhole() throws IOException {
        final var hash = PasswordUtil.generate(stdin("correct horse\n"));

        final var hashing = new PasswordHashingService();
        assertTrue(hashing.verifyPassword("correct horse", hash));
        assertFalse(hashing.verifyPassword("correct", hash));
    }

    @Test
    @DisplayName("Empty standard input is rejected")
    void emptyInputIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.generate(stdin("")));
    }
}
