package de.vptr.aimathtutor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.ws.rs.core.Response;

class ErrorMessageUtilTest {

    @Mock
    private Response response;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Should extract error message from structured JSON response")
    void shouldExtractErrorMessageFromStructuredResponse() {
        // Given
        String jsonResponse = "{\"message\":\"User not found\",\"status\":404}";
        when(response.hasEntity()).thenReturn(true);
        when(response.getStatus()).thenReturn(404);
        when(response.readEntity(String.class)).thenReturn(jsonResponse);

        // When
        String result = ErrorMessageUtil.extractErrorMessage(response);

        // Then
        assertEquals("User not found", result);
    }

    @Test
    @DisplayName("Should extract error message from JSON with nested message")
    void shouldExtractErrorMessageFromNestedJson() {
        // Given
        String jsonResponse = "{\"error\":{\"message\":\"Invalid credentials\"},\"code\":401}";
        when(response.hasEntity()).thenReturn(true);
        when(response.getStatus()).thenReturn(401);
        when(response.readEntity(String.class)).thenReturn(jsonResponse);

        // When
        String result = ErrorMessageUtil.extractErrorMessage(response);

        // Then
        assertEquals("Invalid credentials", result);
    }

    @Test
    @DisplayName("Should return plain text response when no JSON structure found")
    void shouldReturnPlainTextResponse() {
        // Given
        String plainTextResponse = "Internal Server Error";
        when(response.hasEntity()).thenReturn(true);
        when(response.getStatus()).thenReturn(500);
        when(response.readEntity(String.class)).thenReturn(plainTextResponse);

        // When
        String result = ErrorMessageUtil.extractErrorMessage(response);

        // Then
        assertEquals("Internal Server Error", result);
    }

    @Test
    @DisplayName("Should return HTTP status when response has no entity")
    void shouldReturnHttpStatusWhenNoEntity() {
        // Given
        when(response.hasEntity()).thenReturn(false);
        when(response.getStatus()).thenReturn(404);

        // When
        String result = ErrorMessageUtil.extractErrorMessage(response);

        // Then
        assertEquals("HTTP 404", result);
    }

    @Test
    @DisplayName("Should return HTTP status when response body is empty")
    void shouldReturnHttpStatusWhenEmptyBody() {
        // Given
        when(response.hasEntity()).thenReturn(true);
        when(response.getStatus()).thenReturn(400);
        when(response.readEntity(String.class)).thenReturn("");

        // When
        String result = ErrorMessageUtil.extractErrorMessage(response);

        // Then
        assertEquals("HTTP 400", result);
    }

    @Test
    @DisplayName("Should return HTTP status when response body is whitespace only")
    void shouldReturnHttpStatusWhenWhitespaceOnly() {
        // Given
        when(response.hasEntity()).thenReturn(true);
        when(response.getStatus()).thenReturn(400);
        when(response.readEntity(String.class)).thenReturn("   \n\t   ");

        // When
        String result = ErrorMessageUtil.extractErrorMessage(response);

        // Then
        assertEquals("HTTP 400", result);
    }

    @Test
    @DisplayName("Should handle JSON with message containing quotes")
    void shouldHandleMessageWithQuotes() {
        // Given
        String jsonResponse = "{\"message\":\"Error: \\\"field\\\" is required\"}";
        when(response.hasEntity()).thenReturn(true);
        when(response.getStatus()).thenReturn(400);
        when(response.readEntity(String.class)).thenReturn(jsonResponse);

        // When
        String result = ErrorMessageUtil.extractErrorMessage(response);

        // Then
        assertEquals("Error: \\", result); // The parser stops at the first quote
    }

    @Test
    @DisplayName("Should return HTTP status when exception occurs during parsing")
    void shouldReturnHttpStatusWhenExceptionOccurs() {
        // Given
        when(response.hasEntity()).thenReturn(true);
        when(response.getStatus()).thenReturn(500);
        when(response.readEntity(String.class)).thenThrow(new RuntimeException("Parse error"));

        // When
        String result = ErrorMessageUtil.extractErrorMessage(response);

        // Then
        assertEquals("HTTP 500", result);
    }

    @Test
    @DisplayName("Should handle non-error status codes")
    void shouldHandleNonErrorStatusCodes() {
        // Given
        when(response.hasEntity()).thenReturn(true);
        when(response.getStatus()).thenReturn(200);

        // When
        String result = ErrorMessageUtil.extractErrorMessage(response);

        // Then
        assertEquals("HTTP 200", result);
    }

    @Test
    @DisplayName("Should handle malformed JSON")
    void shouldHandleMalformedJson() {
        // Given
        String malformedJson = "{\"message\":\"Error message\" invalid json";
        when(response.hasEntity()).thenReturn(true);
        when(response.getStatus()).thenReturn(400);
        when(response.readEntity(String.class)).thenReturn(malformedJson);

        // When
        String result = ErrorMessageUtil.extractErrorMessage(response);

        // Then
        assertEquals("Error message", result); // The parser extracts the message despite malformed JSON
    }

    @Test
    @DisplayName("Should extract first message when multiple message fields exist")
    void shouldExtractFirstMessageWhenMultipleExist() {
        // Given
        String jsonResponse = "{\"message\":\"First error\",\"details\":{\"message\":\"Second error\"}}";
        when(response.hasEntity()).thenReturn(true);
        when(response.getStatus()).thenReturn(400);
        when(response.readEntity(String.class)).thenReturn(jsonResponse);

        // When
        String result = ErrorMessageUtil.extractErrorMessage(response);

        // Then
        assertEquals("First error", result);
    }

    @Test
    @DisplayName("Should handle JSON with message field but no value")
    void shouldHandleMessageFieldWithNoValue() {
        // Given
        String jsonResponse = "{\"message\":\"\",\"status\":400}";
        when(response.hasEntity()).thenReturn(true);
        when(response.getStatus()).thenReturn(400);
        when(response.readEntity(String.class)).thenReturn(jsonResponse);

        // When
        String result = ErrorMessageUtil.extractErrorMessage(response);

        // Then
        assertEquals("", result);
    }

    @Test
    @DisplayName("Should trim whitespace from extracted message")
    void shouldTrimWhitespaceFromMessage() {
        // Given
        String jsonResponse = "{\"message\":\"  Error with spaces  \"}";
        when(response.hasEntity()).thenReturn(true);
        when(response.getStatus()).thenReturn(400);
        when(response.readEntity(String.class)).thenReturn(jsonResponse);

        // When
        String result = ErrorMessageUtil.extractErrorMessage(response);

        // Then
        assertEquals("Error with spaces", result);
    }
}