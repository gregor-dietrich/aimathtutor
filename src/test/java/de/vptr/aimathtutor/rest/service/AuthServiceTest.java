package de.vptr.aimathtutor.rest.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.aimathtutor.security.PasswordHashingService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private PasswordHashingService passwordHashingService;

    @Mock
    private UserRankService userRankService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Should return invalid input when username is null")
    void shouldReturnInvalidInputWhenUsernameIsNull() {
        var result = authService.authenticate(null, "password");
        assertFalse(result.isSuccess());
        assertEquals("Username and password are required", result.getMessage());
    }

    @Test
    @DisplayName("Should return invalid input when username is empty")
    void shouldReturnInvalidInputWhenUsernameIsEmpty() {
        var result = authService.authenticate("", "password");
        assertFalse(result.isSuccess());
        assertEquals("Username and password are required", result.getMessage());
    }

    @Test
    @DisplayName("Should return invalid input when username is whitespace")
    void shouldReturnInvalidInputWhenUsernameIsWhitespace() {
        var result = authService.authenticate("   ", "password");
        assertFalse(result.isSuccess());
        assertEquals("Username and password are required", result.getMessage());
    }

    @Test
    @DisplayName("Should return invalid input when password is null")
    void shouldReturnInvalidInputWhenPasswordIsNull() {
        var result = authService.authenticate("username", null);
        assertFalse(result.isSuccess());
        assertEquals("Username and password are required", result.getMessage());
    }

    @Test
    @DisplayName("Should return invalid input when password is empty")
    void shouldReturnInvalidInputWhenPasswordIsEmpty() {
        var result = authService.authenticate("username", "");
        assertFalse(result.isSuccess());
        assertEquals("Username and password are required", result.getMessage());
    }

    @Test
    @DisplayName("Should return invalid input when password is whitespace")
    void shouldReturnInvalidInputWhenPasswordIsWhitespace() {
        var result = authService.authenticate("username", "   ");
        assertFalse(result.isSuccess());
        assertEquals("Username and password are required", result.getMessage());
    }
}
