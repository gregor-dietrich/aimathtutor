package de.vptr.aimathtutor.rest.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthResultDtoTest {

    @Test
    @DisplayName("Should create successful auth result")
    void shouldCreateSuccessfulAuthResult() {
        // When
        AuthResultDto result = AuthResultDto.success();

        // Then
        assertEquals(AuthResultDto.Status.SUCCESS, result.getStatus());
        assertEquals("Authentication successful", result.getMessage());
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should create invalid credentials result")
    void shouldCreateInvalidCredentialsResult() {
        // When
        AuthResultDto result = AuthResultDto.invalidCredentials();

        // Then
        assertEquals(AuthResultDto.Status.INVALID_CREDENTIALS, result.getStatus());
        assertEquals("Invalid username or password", result.getMessage());
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Should create backend unavailable result")
    void shouldCreateBackendUnavailableResult() {
        // Given
        String details = "Connection timeout";

        // When
        AuthResultDto result = AuthResultDto.backendUnavailable(details);

        // Then
        assertEquals(AuthResultDto.Status.BACKEND_UNAVAILABLE, result.getStatus());
        assertEquals("Backend service unavailable: Connection timeout", result.getMessage());
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Should create invalid input result")
    void shouldCreateInvalidInputResult() {
        // When
        AuthResultDto result = AuthResultDto.invalidInput();

        // Then
        assertEquals(AuthResultDto.Status.INVALID_INPUT, result.getStatus());
        assertEquals("Username and password are required", result.getMessage());
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Should handle null details in backend unavailable")
    void shouldHandleNullDetailsInBackendUnavailable() {
        // When
        AuthResultDto result = AuthResultDto.backendUnavailable(null);

        // Then
        assertEquals(AuthResultDto.Status.BACKEND_UNAVAILABLE, result.getStatus());
        assertEquals("Backend service unavailable: null", result.getMessage());
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Should handle empty details in backend unavailable")
    void shouldHandleEmptyDetailsInBackendUnavailable() {
        // When
        AuthResultDto result = AuthResultDto.backendUnavailable("");

        // Then
        assertEquals(AuthResultDto.Status.BACKEND_UNAVAILABLE, result.getStatus());
        assertEquals("Backend service unavailable: ", result.getMessage());
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Should have immutable status and message")
    void shouldHaveImmutableStatusAndMessage() {
        // Given
        AuthResultDto result = AuthResultDto.success();
        AuthResultDto.Status originalStatus = result.getStatus();
        String originalMessage = result.getMessage();

        // When - Try to access multiple times
        AuthResultDto.Status status1 = result.getStatus();
        String message1 = result.getMessage();
        AuthResultDto.Status status2 = result.getStatus();
        String message2 = result.getMessage();

        // Then - Should be consistent
        assertEquals(originalStatus, status1);
        assertEquals(originalStatus, status2);
        assertEquals(originalMessage, message1);
        assertEquals(originalMessage, message2);
    }

    @Test
    @DisplayName("Should test all status enum values")
    void shouldTestAllStatusEnumValues() {
        // Test all enum values exist
        AuthResultDto.Status[] expectedValues = {
                AuthResultDto.Status.SUCCESS,
                AuthResultDto.Status.INVALID_CREDENTIALS,
                AuthResultDto.Status.BACKEND_UNAVAILABLE,
                AuthResultDto.Status.INVALID_INPUT
        };

        AuthResultDto.Status[] actualValues = AuthResultDto.Status.values();

        assertEquals(expectedValues.length, actualValues.length);
        for (AuthResultDto.Status expected : expectedValues) {
            boolean found = false;
            for (AuthResultDto.Status actual : actualValues) {
                if (expected == actual) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Status " + expected + " not found in enum");
        }
    }
}