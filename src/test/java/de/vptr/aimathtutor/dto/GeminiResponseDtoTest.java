package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GeminiResponseDtoTest {

    @Test
    @DisplayName("isBlocked should return false for empty response")
    void isBlockedShouldReturnFalseForEmptyResponse() {
        final var dto = new GeminiResponseDto();
        dto.candidates = null;
        assertFalse(dto.isBlocked());
    }

    @Test
    @DisplayName("isBlocked should return true for SAFETY finish reason")
    void isBlockedShouldReturnTrueForSafety() {
        final var dto = new GeminiResponseDto();
        final var candidate = new GeminiResponseDto.Candidate();
        candidate.finishReason = "SAFETY";
        dto.candidates = List.of(candidate);
        assertTrue(dto.isBlocked());
    }

    @Test
    @DisplayName("isBlocked should return true for BLOCKED finish reason")
    void isBlockedShouldReturnTrueForBlocked() {
        final var dto = new GeminiResponseDto();
        final var candidate = new GeminiResponseDto.Candidate();
        candidate.finishReason = "BLOCKED";
        dto.candidates = List.of(candidate);
        assertTrue(dto.isBlocked());
    }

    @Test
    @DisplayName("isBlocked should return false for STOP finish reason")
    void isBlockedShouldReturnFalseForStop() {
        final var dto = new GeminiResponseDto();
        final var candidate = new GeminiResponseDto.Candidate();
        candidate.finishReason = "STOP";
        dto.candidates = List.of(candidate);
        assertFalse(dto.isBlocked());
    }

    @Test
    @DisplayName("isEmptyResponse should return true when candidates is null")
    void isEmptyResponseShouldReturnTrueForNullCandidates() {
        final var dto = new GeminiResponseDto();
        dto.candidates = null;
        assertTrue(dto.isEmptyResponse());
    }

    @Test
    @DisplayName("isEmptyResponse should return true when candidates is empty")
    void isEmptyResponseShouldReturnTrueForEmptyCandidates() {
        final var dto = new GeminiResponseDto();
        dto.candidates = List.of();
        assertTrue(dto.isEmptyResponse());
    }

    @Test
    @DisplayName("isEmptyResponse should return false when candidates exist")
    void isEmptyResponseShouldReturnFalseForExistingCandidates() {
        final var dto = new GeminiResponseDto();
        final var candidate = new GeminiResponseDto.Candidate();
        dto.candidates = List.of(candidate);
        assertFalse(dto.isEmptyResponse());
    }
}
