package de.vptr.aimathtutor.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordHashingServiceTest {

    private PasswordHashingService passwordHashingService;

    @BeforeEach
    void setUp() {
        passwordHashingService = new PasswordHashingService();
    }

    @Test
    @DisplayName("Should generate unique salts")
    void shouldGenerateUniqueSalts() {
        // When
        String salt1 = passwordHashingService.generateSalt();
        String salt2 = passwordHashingService.generateSalt();

        // Then
        assertNotNull(salt1);
        assertNotNull(salt2);
        assertNotEquals(salt1, salt2);
        assertFalse(salt1.isEmpty());
        assertFalse(salt2.isEmpty());
    }

    @Test
    @DisplayName("Should generate salt of expected length")
    void shouldGenerateSaltOfExpectedLength() {
        // When
        String salt = passwordHashingService.generateSalt();

        // Then
        // Base64 encoding of 32 bytes should result in 44 characters (with padding)
        assertTrue(salt.length() >= 40); // Allow some variance for Base64 padding
    }

    @Test
    @DisplayName("Should hash password successfully")
    void shouldHashPasswordSuccessfully() throws Exception {
        // Given
        String password = "testPassword123";
        String salt = passwordHashingService.generateSalt();

        // When
        String hashedPassword = passwordHashingService.hashPassword(password, salt);

        // Then
        assertNotNull(hashedPassword);
        assertFalse(hashedPassword.isEmpty());
        assertNotEquals(password, hashedPassword);
    }

    @Test
    @DisplayName("Should produce different hashes for same password with different salts")
    void shouldProduceDifferentHashesForSamePasswordWithDifferentSalts() throws Exception {
        // Given
        String password = "testPassword123";
        String salt1 = passwordHashingService.generateSalt();
        String salt2 = passwordHashingService.generateSalt();

        // When
        String hash1 = passwordHashingService.hashPassword(password, salt1);
        String hash2 = passwordHashingService.hashPassword(password, salt2);

        // Then
        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Should produce same hash for same password and salt")
    void shouldProduceSameHashForSamePasswordAndSalt() throws Exception {
        // Given
        String password = "testPassword123";
        String salt = passwordHashingService.generateSalt();

        // When
        String hash1 = passwordHashingService.hashPassword(password, salt);
        String hash2 = passwordHashingService.hashPassword(password, salt);

        // Then
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Should verify correct password")
    void shouldVerifyCorrectPassword() throws Exception {
        // Given
        String password = "testPassword123";
        String salt = passwordHashingService.generateSalt();
        String storedHash = passwordHashingService.hashPassword(password, salt);

        // When
        boolean isValid = passwordHashingService.verifyPassword(password, storedHash, salt);

        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should reject incorrect password")
    void shouldRejectIncorrectPassword() throws Exception {
        // Given
        String correctPassword = "testPassword123";
        String incorrectPassword = "wrongPassword";
        String salt = passwordHashingService.generateSalt();
        String storedHash = passwordHashingService.hashPassword(correctPassword, salt);

        // When
        boolean isValid = passwordHashingService.verifyPassword(incorrectPassword, storedHash, salt);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle null password in verification")
    void shouldHandleNullPasswordInVerification() {
        // Given
        String salt = passwordHashingService.generateSalt();

        // When
        boolean isValid = passwordHashingService.verifyPassword(null, "someHash", salt);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle null hash in verification")
    void shouldHandleNullHashInVerification() {
        // Given
        String salt = passwordHashingService.generateSalt();

        // When
        boolean isValid = passwordHashingService.verifyPassword("password", null, salt);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle null salt in verification")
    void shouldHandleNullSaltInVerification() {
        // When
        boolean isValid = passwordHashingService.verifyPassword("password", "hash", null);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle empty password in verification")
    void shouldHandleEmptyPasswordInVerification() {
        // Given
        String salt = passwordHashingService.generateSalt();

        // When
        boolean isValid = passwordHashingService.verifyPassword("", "someHash", salt);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle empty hash in verification")
    void shouldHandleEmptyHashInVerification() {
        // Given
        String salt = passwordHashingService.generateSalt();

        // When
        boolean isValid = passwordHashingService.verifyPassword("password", "", salt);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle empty salt in verification")
    void shouldHandleEmptySaltInVerification() {
        // When
        boolean isValid = passwordHashingService.verifyPassword("password", "hash", "");

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle special characters in password")
    void shouldHandleSpecialCharactersInPassword() throws Exception {
        // Given
        String password = "pàssw@rd!#$%^&*()_+{}|:<>?[]\\;',./~`";
        String salt = passwordHashingService.generateSalt();

        // When
        String hash = passwordHashingService.hashPassword(password, salt);
        boolean isValid = passwordHashingService.verifyPassword(password, hash, salt);

        // Then
        assertNotNull(hash);
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should handle very long passwords")
    void shouldHandleVeryLongPasswords() throws Exception {
        // Given
        String longPassword = "a".repeat(1000);
        String salt = passwordHashingService.generateSalt();

        // When
        String hash = passwordHashingService.hashPassword(longPassword, salt);
        boolean isValid = passwordHashingService.verifyPassword(longPassword, hash, salt);

        // Then
        assertNotNull(hash);
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should handle Unicode characters in password")
    void shouldHandleUnicodeCharactersInPassword() throws Exception {
        // Given
        String unicodePassword = "pássw@rd中文🌟";
        String salt = passwordHashingService.generateSalt();

        // When
        String hash = passwordHashingService.hashPassword(unicodePassword, salt);
        boolean isValid = passwordHashingService.verifyPassword(unicodePassword, hash, salt);

        // Then
        assertNotNull(hash);
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should return false when hash verification throws exception")
    void shouldReturnFalseWhenHashVerificationThrowsException() {
        // Given - invalid Base64 salt that will cause exception during hashing
        String invalidSalt = "invalid-base64!!!";

        // When
        boolean result = passwordHashingService.verifyPassword("password", "hash", invalidSalt);

        // Then
        assertFalse(result);
    }
}