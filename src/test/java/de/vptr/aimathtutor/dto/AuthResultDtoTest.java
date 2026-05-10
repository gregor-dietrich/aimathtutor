package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.AuthResultDto.Status;

@SuppressWarnings("NullAway")
class AuthResultDtoTest {

    @Test
    @DisplayName("success factory method")
    void testSuccess() {
        final var result = AuthResultDto.success();
        assertEquals(Status.SUCCESS, result.getStatus());
        assertEquals("Authentication successful", result.getMessage());
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("invalidCredentials factory method")
    void testInvalidCredentials() {
        final var result = AuthResultDto.invalidCredentials();
        assertEquals(Status.INVALID_CREDENTIALS, result.getStatus());
        assertEquals("Invalid username or password", result.getMessage());
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("backendUnavailable factory method")
    void testBackendUnavailable() {
        final var result = AuthResultDto.backendUnavailable("timeout");
        assertEquals(Status.BACKEND_UNAVAILABLE, result.getStatus());
        assertEquals("Backend service unavailable: timeout", result.getMessage());
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("invalidInput factory method")
    void testInvalidInput() {
        final var result = AuthResultDto.invalidInput();
        assertEquals(Status.INVALID_INPUT, result.getStatus());
        assertEquals("Username and password are required", result.getMessage());
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Status enum values")
    void testStatusValues() {
        assertEquals(4, Status.values().length);
        assertEquals("SUCCESS", Status.SUCCESS.name());
        assertEquals("INVALID_CREDENTIALS", Status.INVALID_CREDENTIALS.name());
        assertEquals("BACKEND_UNAVAILABLE", Status.BACKEND_UNAVAILABLE.name());
        assertEquals("INVALID_INPUT", Status.INVALID_INPUT.name());
    }
}
