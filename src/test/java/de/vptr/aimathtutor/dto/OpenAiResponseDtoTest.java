package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class OpenAiResponseDtoTest {

    @Test
    @DisplayName("isComplete should return true for stop finish reason")
    void isCompleteShouldReturnTrueForStop() {
        final var dto = new OpenAiResponseDto();
        final var choice = new OpenAiResponseDto.Choice();
        choice.finishReason = "stop";
        dto.choices = List.of(choice);
        assertTrue(dto.isComplete());
    }

    @Test
    @DisplayName("isComplete should return false for length finish reason")
    void isCompleteShouldReturnFalseForLength() {
        final var dto = new OpenAiResponseDto();
        final var choice = new OpenAiResponseDto.Choice();
        choice.finishReason = "length";
        dto.choices = List.of(choice);
        assertFalse(dto.isComplete());
    }

    @Test
    @DisplayName("isTruncated should return true for length finish reason")
    void isTruncatedShouldReturnTrueForLength() {
        final var dto = new OpenAiResponseDto();
        final var choice = new OpenAiResponseDto.Choice();
        choice.finishReason = "length";
        dto.choices = List.of(choice);
        assertTrue(dto.isTruncated());
    }

    @Test
    @DisplayName("isContentFiltered should return true for content_filter finish reason")
    void isContentFilteredShouldReturnTrueForContentFilter() {
        final var dto = new OpenAiResponseDto();
        final var choice = new OpenAiResponseDto.Choice();
        choice.finishReason = "content_filter";
        dto.choices = List.of(choice);
        assertTrue(dto.isContentFiltered());
    }

    @Test
    @DisplayName("isContentFiltered should return false for stop finish reason")
    void isContentFilteredShouldReturnFalseForStop() {
        final var dto = new OpenAiResponseDto();
        final var choice = new OpenAiResponseDto.Choice();
        choice.finishReason = "stop";
        dto.choices = List.of(choice);
        assertFalse(dto.isContentFiltered());
    }

    @Test
    @DisplayName("All finish reason checks should handle null choices")
    void shouldHandleNullChoices() {
        final var dto = new OpenAiResponseDto();
        dto.choices = null;
        assertFalse(dto.isComplete());
        assertFalse(dto.isTruncated());
        assertFalse(dto.isContentFiltered());
    }
}
