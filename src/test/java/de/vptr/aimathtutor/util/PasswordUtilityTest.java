package de.vptr.aimathtutor.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.security.PasswordHashingService;

public class PasswordUtilityTest {

    @Test
    public void testGenerateAndVerify() throws Exception {
        final var service = new PasswordHashingService();
        final var password = "testPassword123";

        final var salt = service.generateSalt();
        assertNotNull(salt);

        final var hash = service.hashPassword(password, salt);
        assertNotNull(hash);

        assertTrue(service.verifyPassword(password, hash, salt));
    }
}
