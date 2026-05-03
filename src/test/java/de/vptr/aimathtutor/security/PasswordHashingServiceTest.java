package de.vptr.aimathtutor.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordHashingServiceTest {

    private PasswordHashingService passwordHashingService;

    @BeforeEach
    void setUp() {
        this.passwordHashingService = new PasswordHashingService();
    }

    @Test
    @DisplayName("Should hash password successfully")
    void shouldHashPasswordSuccessfully() {
        // Given
        final String password = "testPassword123";

        // When
        final String hashedPassword = this.passwordHashingService.hashPassword(password);

        // Then
        assertNotNull(hashedPassword);
        assertFalse(hashedPassword.isEmpty());
        assertNotEquals(password, hashedPassword);
        // Bcrypt hashes start with $2a$ or $2b$ or $2y$
        assertTrue(hashedPassword.startsWith("$2"));
    }

    @Test
    @DisplayName("Should produce different hashes for same password")
    void shouldProduceDifferentHashesForSamePassword() {
        // Given
        final String password = "testPassword123";

        // When
        final String hash1 = this.passwordHashingService.hashPassword(password);
        final String hash2 = this.passwordHashingService.hashPassword(password);

        // Then
        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Should verify correct password")
    void shouldVerifyCorrectPassword() {
        // Given
        final String password = "testPassword123";
        final String storedHash = this.passwordHashingService.hashPassword(password);

        // When
        final boolean isValid = this.passwordHashingService.verifyPassword(password, storedHash);

        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should reject incorrect password")
    void shouldRejectIncorrectPassword() {
        // Given
        final String correctPassword = "testPassword123";
        final String incorrectPassword = "wrongPassword";
        final String storedHash = this.passwordHashingService.hashPassword(correctPassword);

        // When
        final boolean isValid = this.passwordHashingService.verifyPassword(incorrectPassword, storedHash);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle null password in verification")
    void shouldHandleNullPasswordInVerification() {
        // When
        final boolean isValid = this.passwordHashingService.verifyPassword(null, "someHash");

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle null hash in verification")
    void shouldHandleNullHashInVerification() {
        // When
        final boolean isValid = this.passwordHashingService.verifyPassword("password", null);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle empty password in verification")
    void shouldHandleEmptyPasswordInVerification() {
        // When
        final boolean isValid = this.passwordHashingService.verifyPassword("", "someHash");

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle empty hash in verification")
    void shouldHandleEmptyHashInVerification() {
        // When
        final boolean isValid = this.passwordHashingService.verifyPassword("password", "");

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle special characters in password")
    void shouldHandleSpecialCharactersInPassword() {
        // Given
        final String password = "pàssw@rd!#$%^&*()_+{}|:<>?[]\\;',./~`";

        // When
        final String hash = this.passwordHashingService.hashPassword(password);
        final boolean isValid = this.passwordHashingService.verifyPassword(password, hash);

        // Then
        assertNotNull(hash);
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should reject passwords exceeding 72 bytes")
    void shouldRejectVeryLongPasswords() {
        final String longPassword = "a".repeat(1000);
        assertThrows(IllegalArgumentException.class,
                () -> this.passwordHashingService.hashPassword(longPassword));
    }

    @Test
    @DisplayName("Should handle Unicode characters in password")
    void shouldHandleUnicodeCharactersInPassword() {
        // Given
        final String unicodePassword = "pássw@rd中文🌟";

        // When
        final String hash = this.passwordHashingService.hashPassword(unicodePassword);
        final boolean isValid = this.passwordHashingService.verifyPassword(unicodePassword, hash);

        // Then
        assertNotNull(hash);
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should reject invalid bcrypt hash")
    void shouldRejectInvalidBcryptHash() {
        // Given
        final String invalidHash = "not-a-valid-bcrypt-hash";

        // When
        final boolean result = this.passwordHashingService.verifyPassword("password", invalidHash);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should throw on null password when hashing")
    void shouldThrowOnNullPasswordWhenHashing() {
        assertThrows(IllegalArgumentException.class, () -> this.passwordHashingService.hashPassword(null));
    }

    @Test
    @DisplayName("Should throw on empty password when hashing")
    void shouldThrowOnEmptyPasswordWhenHashing() {
        assertThrows(IllegalArgumentException.class, () -> this.passwordHashingService.hashPassword(""));
    }
}
