package de.vptr.aimathtutor.security;

import static org.junit.jupiter.api.Assertions.*;

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
    @DisplayName("Should generate unique salts")
    void shouldGenerateUniqueSalts() {
        // When
        final String salt1 = this.passwordHashingService.generateSalt();
        final String salt2 = this.passwordHashingService.generateSalt();

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
        final String salt = this.passwordHashingService.generateSalt();

        // Then
        // Base64 encoding of 32 bytes should result in 44 characters (with padding)
        assertTrue(salt.length() >= 40); // Allow some variance for Base64 padding
    }

    @Test
    @DisplayName("Should hash password successfully")
    void shouldHashPasswordSuccessfully() throws Exception {
        // Given
        final String password = "testPassword123";
        final String salt = this.passwordHashingService.generateSalt();

        // When
        final String hashedPassword = this.passwordHashingService.hashPassword(password, salt);

        // Then
        assertNotNull(hashedPassword);
        assertFalse(hashedPassword.isEmpty());
        assertNotEquals(password, hashedPassword);
    }

    @Test
    @DisplayName("Should produce different hashes for same password with different salts")
    void shouldProduceDifferentHashesForSamePasswordWithDifferentSalts() throws Exception {
        // Given
        final String password = "testPassword123";
        final String salt1 = this.passwordHashingService.generateSalt();
        final String salt2 = this.passwordHashingService.generateSalt();

        // When
        final String hash1 = this.passwordHashingService.hashPassword(password, salt1);
        final String hash2 = this.passwordHashingService.hashPassword(password, salt2);

        // Then
        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Should produce same hash for same password and salt")
    void shouldProduceSameHashForSamePasswordAndSalt() throws Exception {
        // Given
        final String password = "testPassword123";
        final String salt = this.passwordHashingService.generateSalt();

        // When
        final String hash1 = this.passwordHashingService.hashPassword(password, salt);
        final String hash2 = this.passwordHashingService.hashPassword(password, salt);

        // Then
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Should verify correct password")
    void shouldVerifyCorrectPassword() throws Exception {
        // Given
        final String password = "testPassword123";
        final String salt = this.passwordHashingService.generateSalt();
        final String storedHash = this.passwordHashingService.hashPassword(password, salt);

        // When
        final boolean isValid = this.passwordHashingService.verifyPassword(password, storedHash, salt);

        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should reject incorrect password")
    void shouldRejectIncorrectPassword() throws Exception {
        // Given
        final String correctPassword = "testPassword123";
        final String incorrectPassword = "wrongPassword";
        final String salt = this.passwordHashingService.generateSalt();
        final String storedHash = this.passwordHashingService.hashPassword(correctPassword, salt);

        // When
        final boolean isValid = this.passwordHashingService.verifyPassword(incorrectPassword, storedHash, salt);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle null password in verification")
    void shouldHandleNullPasswordInVerification() {
        // Given
        final String salt = this.passwordHashingService.generateSalt();

        // When
        final boolean isValid = this.passwordHashingService.verifyPassword(null, "someHash", salt);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle null hash in verification")
    void shouldHandleNullHashInVerification() {
        // Given
        final String salt = this.passwordHashingService.generateSalt();

        // When
        final boolean isValid = this.passwordHashingService.verifyPassword("password", null, salt);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle null salt in verification")
    void shouldHandleNullSaltInVerification() {
        // When
        final boolean isValid = this.passwordHashingService.verifyPassword("password", "hash", null);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle empty password in verification")
    void shouldHandleEmptyPasswordInVerification() {
        // Given
        final String salt = this.passwordHashingService.generateSalt();

        // When
        final boolean isValid = this.passwordHashingService.verifyPassword("", "someHash", salt);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle empty hash in verification")
    void shouldHandleEmptyHashInVerification() {
        // Given
        final String salt = this.passwordHashingService.generateSalt();

        // When
        final boolean isValid = this.passwordHashingService.verifyPassword("password", "", salt);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle empty salt in verification")
    void shouldHandleEmptySaltInVerification() {
        // When
        final boolean isValid = this.passwordHashingService.verifyPassword("password", "hash", "");

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle special characters in password")
    void shouldHandleSpecialCharactersInPassword() throws Exception {
        // Given
        final String password = "pàssw@rd!#$%^&*()_+{}|:<>?[]\\;',./~`";
        final String salt = this.passwordHashingService.generateSalt();

        // When
        final String hash = this.passwordHashingService.hashPassword(password, salt);
        final boolean isValid = this.passwordHashingService.verifyPassword(password, hash, salt);

        // Then
        assertNotNull(hash);
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should handle very long passwords")
    void shouldHandleVeryLongPasswords() throws Exception {
        // Given
        final String longPassword = "a".repeat(1000);
        final String salt = this.passwordHashingService.generateSalt();

        // When
        final String hash = this.passwordHashingService.hashPassword(longPassword, salt);
        final boolean isValid = this.passwordHashingService.verifyPassword(longPassword, hash, salt);

        // Then
        assertNotNull(hash);
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should handle Unicode characters in password")
    void shouldHandleUnicodeCharactersInPassword() throws Exception {
        // Given
        final String unicodePassword = "pássw@rd中文🌟";
        final String salt = this.passwordHashingService.generateSalt();

        // When
        final String hash = this.passwordHashingService.hashPassword(unicodePassword, salt);
        final boolean isValid = this.passwordHashingService.verifyPassword(unicodePassword, hash, salt);

        // Then
        assertNotNull(hash);
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should return false when hash verification throws exception")
    void shouldReturnFalseWhenHashVerificationThrowsException() {
        // Given - invalid Base64 salt that will cause exception during hashing
        final String invalidSalt = "invalid-base64!!!";

        // When
        final boolean result = this.passwordHashingService.verifyPassword("password", "hash", invalidSalt);

        // Then
        assertFalse(result);
    }
}