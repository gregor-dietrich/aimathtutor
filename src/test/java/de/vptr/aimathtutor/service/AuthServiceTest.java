package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.AuthResultDto;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class AuthServiceTest {

    @Inject
    private AuthService authService;

    @Test
    @DisplayName("Should return invalid input when username is null")
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
}
