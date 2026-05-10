package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NullAway")
class OllamaResponseDtoTest {

    @Test
    @DisplayName("Default constructor has null fields")
    void testDefaultConstructor() {
        final var dto = new OllamaResponseDto();
        assertNull(dto.model);
        assertNull(dto.createdAt);
        assertNull(dto.response);
        assertNull(dto.done);
        assertNull(dto.doneReason);
        assertNull(dto.totalDuration);
        assertNull(dto.loadDuration);
        assertNull(dto.promptEvalCount);
        assertNull(dto.evalCount);
        assertNull(dto.evalDuration);
    }

    @Test
    @DisplayName("getTextContent returns response")
    void testGetTextContent() {
        final var dto = new OllamaResponseDto();
        dto.response = "Hello world";
        assertEquals("Hello world", dto.getTextContent());
    }

    @Test
    @DisplayName("isComplete returns true when done")
    void testIsCompleteTrue() {
        final var dto = new OllamaResponseDto();
        dto.done = true;
        assertTrue(dto.isComplete());
    }

    @Test
    @DisplayName("isComplete returns false when not done")
    void testIsCompleteFalse() {
        final var dto = new OllamaResponseDto();
        dto.done = false;
        assertFalse(dto.isComplete());
    }

    @Test
    @DisplayName("isComplete returns false when null")
    void testIsCompleteNull() {
        final var dto = new OllamaResponseDto();
        assertFalse(dto.isComplete());
    }

    @Test
    @DisplayName("isTruncated returns true for length reason")
    void testIsTruncatedTrue() {
        final var dto = new OllamaResponseDto();
        dto.doneReason = "length";
        assertTrue(dto.isTruncated());
    }

    @Test
    @DisplayName("isTruncated is case-insensitive")
    void testIsTruncatedCaseInsensitive() {
        final var dto = new OllamaResponseDto();
        dto.doneReason = "LENGTH";
        assertTrue(dto.isTruncated());
    }

    @Test
    @DisplayName("isTruncated returns false for other reasons")
    void testIsTruncatedFalse() {
        final var dto = new OllamaResponseDto();
        dto.doneReason = "stop";
        assertFalse(dto.isTruncated());
    }

    @Test
    @DisplayName("getTokensPerSecond calculates correctly")
    void testGetTokensPerSecond() {
        final var dto = new OllamaResponseDto();
        dto.evalCount = 100;
        dto.evalDuration = 1_000_000_000L; // 1 second
        assertEquals(100.0, dto.getTokensPerSecond());
    }

    @Test
    @DisplayName("getTokensPerSecond returns null when missing data")
    void testGetTokensPerSecondNull() {
        final var dto = new OllamaResponseDto();
        assertNull(dto.getTokensPerSecond());
    }

    @Test
    @DisplayName("getTokensPerSecond returns null when zero duration")
    void testGetTokensPerSecondZeroDuration() {
        final var dto = new OllamaResponseDto();
        dto.evalCount = 100;
        dto.evalDuration = 0L;
        assertNull(dto.getTokensPerSecond());
    }
}
