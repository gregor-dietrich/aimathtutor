package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.AuthResultDto;
import de.vptr.aimathtutor.repository.UserRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class AuthServiceTest {

    @Inject
    private AuthService authService;

    @Inject
    private UserRepository userRepository;

    @Inject
    private LoginAttemptService loginAttemptService;

    @Test
    @DisplayName("Should return invalid input when username is null")
    @SuppressWarnings("NullAway")
    void shouldReturnInvalidInputWhenUsernameIsNull() {
        final var result = this.authService.authenticate(null, "password");
        assertFalse(result.isSuccess());
        assertEquals("Username and password are required", result.getMessage());
    }

    @Test
    @DisplayName("Should return invalid input when username is empty")
    void shouldReturnInvalidInputWhenUsernameIsEmpty() {
        final var result = this.authService.authenticate("", "password");
        assertFalse(result.isSuccess());
        assertEquals("Username and password are required", result.getMessage());
    }

    @Test
    @DisplayName("Should return invalid input when username is whitespace")
    void shouldReturnInvalidInputWhenUsernameIsWhitespace() {
        final var result = this.authService.authenticate("   ", "password");
        assertFalse(result.isSuccess());
        assertEquals("Username and password are required", result.getMessage());
    }

    @Test
    @DisplayName("Should return invalid input when password is null")
    @SuppressWarnings("NullAway")
    void shouldReturnInvalidInputWhenPasswordIsNull() {
        final var result = this.authService.authenticate("username", null);
        assertFalse(result.isSuccess());
        assertEquals("Username and password are required", result.getMessage());
    }

    @Test
    @DisplayName("Should return invalid input when password is empty")
    void shouldReturnInvalidInputWhenPasswordIsEmpty() {
        final var result = this.authService.authenticate("username", "");
        assertFalse(result.isSuccess());
        assertEquals("Username and password are required", result.getMessage());
    }

    @Test
    @DisplayName("Should return invalid input when password is whitespace")
    void shouldReturnInvalidInputWhenPasswordIsWhitespace() {
        final var result = this.authService.authenticate("username", "   ");
        assertFalse(result.isSuccess());
        assertEquals("Username and password are required", result.getMessage());
    }

    @Test
    @DisplayName("Should authenticate valid seeded user")
    @TestTransaction
    void shouldAuthenticateValidSeededUser() {
        final AuthResultDto result = this.authService.authenticate("admin", "admin");
        assertTrue(result.isSuccess(), "Expected success but got: " + result.getMessage());
        assertEquals("Authentication successful", result.getMessage());
    }

    @Test
    @DisplayName("Should reject wrong password")
    @TestTransaction
    void shouldRejectWrongPassword() {
        final AuthResultDto result = this.authService.authenticate("admin", "wrongpassword");
        assertFalse(result.isSuccess());
        assertEquals("Invalid username or password", result.getMessage());
    }

    @Test
    @DisplayName("Should reject non-existent user")
    @TestTransaction
    void shouldRejectNonExistentUser() {
        final AuthResultDto result = this.authService.authenticate("nonexistent", "password");
        assertFalse(result.isSuccess());
        assertEquals("Invalid username or password", result.getMessage());
    }

    @Test
    @DisplayName("Should reject banned user")
    @TestTransaction
    void shouldRejectBannedUser() {
        final var student = this.userRepository.findByUsername("student1");
        assertNotNull(student, "Seeded student1 must exist");
        student.banned = true;
        this.userRepository.persist(student);

        final AuthResultDto result = this.authService.authenticate("student1", "student1");
        assertFalse(result.isSuccess());
        assertEquals("Invalid username or password", result.getMessage());
    }

    @Test
    @DisplayName("Should reject non-activated user")
    @TestTransaction
    void shouldRejectNonActivatedUser() {
        final var student = this.userRepository.findByUsername("student2");
        assertNotNull(student, "Seeded student2 must exist");
        student.activated = false;
        this.userRepository.persist(student);

        final AuthResultDto result = this.authService.authenticate("student2", "student2");
        assertFalse(result.isSuccess());
        assertEquals("Invalid username or password", result.getMessage());
    }

    @Test
    @DisplayName("Should throttle after too many failed attempts")
    void shouldThrottleAfterTooManyFailedAttempts() {
        final String uniqueKey = "throttle_test_" + UUID.randomUUID().toString().substring(0, 8);
        for (int i = 0; i < 5; i++) {
            this.loginAttemptService.recordFailedAttempt(uniqueKey);
        }
        assertTrue(this.loginAttemptService.isLockedOut(uniqueKey));

        final AuthResultDto result = this.authService.authenticate(uniqueKey, "anypassword");
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Too many failed attempts"),
                "Expected throttle message, got: " + result.getMessage());

        this.loginAttemptService.recordSuccessfulLogin(uniqueKey);
    }
}
