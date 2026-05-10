package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OpenAiResponseDtoTest {

    @Test
    @DisplayName("getTextContent returns content from first choice")
    void testGetTextContent() {
        final var dto = new OpenAiResponseDto();
        final var message = new OpenAiResponseDto.Message();
        message.role = "assistant";
        message.content = "Hello!";
        final var choice = new OpenAiResponseDto.Choice();
        choice.message = message;
        choice.finishReason = "stop";
        dto.choices = List.of(choice);

        assertEquals("Hello!", dto.getTextContent());
    }

    @Test
    @DisplayName("getTextContent returns null when choices is null")
    void testGetTextContentNullChoices() {
        final var dto = new OpenAiResponseDto();
        assertNull(dto.getTextContent());
    }

    @Test
    @DisplayName("getTextContent returns null when choices is empty")
    void testGetTextContentEmptyChoices() {
        final var dto = new OpenAiResponseDto();
        dto.choices = List.of();
        assertNull(dto.getTextContent());
    }

    @Test
    @DisplayName("getTextContent returns null when message is null")
    void testGetTextContentNullMessage() {
        final var dto = new OpenAiResponseDto();
        final var choice = new OpenAiResponseDto.Choice();
        choice.message = null;
        dto.choices = List.of(choice);
        assertNull(dto.getTextContent());
    }

    @Test
    @DisplayName("getTextContent returns null when content is null")
    void testGetTextContentNullContent() {
        final var dto = new OpenAiResponseDto();
        final var message = new OpenAiResponseDto.Message();
        message.content = null;
        final var choice = new OpenAiResponseDto.Choice();
        choice.message = message;
        dto.choices = List.of(choice);
        assertNull(dto.getTextContent());
    }

    @Test
    @DisplayName("isComplete returns true for stop finish reason")
    void testIsComplete() {
        final var dto = new OpenAiResponseDto();
        final var choice = new OpenAiResponseDto.Choice();
        choice.finishReason = "stop";
        dto.choices = List.of(choice);
        assertTrue(dto.isComplete());
    }

    @Test
    @DisplayName("isComplete returns false for non-stop finish reason")
    void testIsNotComplete() {
        final var dto = new OpenAiResponseDto();
        final var choice = new OpenAiResponseDto.Choice();
        choice.finishReason = "length";
        dto.choices = List.of(choice);
        assertFalse(dto.isComplete());
    }

    @Test
    @DisplayName("isComplete returns false when choices is null")
    void testIsCompleteNullChoices() {
        final var dto = new OpenAiResponseDto();
        assertFalse(dto.isComplete());
    }

    @Test
    @DisplayName("isTruncated returns true for length finish reason")
    void testIsTruncated() {
        final var dto = new OpenAiResponseDto();
        final var choice = new OpenAiResponseDto.Choice();
        choice.finishReason = "length";
        dto.choices = List.of(choice);
        assertTrue(dto.isTruncated());
    }

    @Test
    @DisplayName("isTruncated returns false for stop finish reason")
    void testIsNotTruncated() {
        final var dto = new OpenAiResponseDto();
        final var choice = new OpenAiResponseDto.Choice();
        choice.finishReason = "stop";
        dto.choices = List.of(choice);
        assertFalse(dto.isTruncated());
    }

    @Test
    @DisplayName("isTruncated returns false when choices is null")
    void testIsTruncatedNullChoices() {
        final var dto = new OpenAiResponseDto();
        assertFalse(dto.isTruncated());
    }

    @Test
    @DisplayName("isContentFiltered returns true for content_filter finish reason")
    void testIsContentFiltered() {
        final var dto = new OpenAiResponseDto();
        final var choice = new OpenAiResponseDto.Choice();
        choice.finishReason = "content_filter";
        dto.choices = List.of(choice);
        assertTrue(dto.isContentFiltered());
    }

    @Test
    @DisplayName("isContentFiltered returns false for stop finish reason")
    void testIsNotContentFiltered() {
        final var dto = new OpenAiResponseDto();
        final var choice = new OpenAiResponseDto.Choice();
        choice.finishReason = "stop";
        dto.choices = List.of(choice);
        assertFalse(dto.isContentFiltered());
    }

    @Test
    @DisplayName("isContentFiltered returns false when choices is null")
    void testIsContentFilteredNullChoices() {
        final var dto = new OpenAiResponseDto();
        assertFalse(dto.isContentFiltered());
    }

    @Test
    @DisplayName("Usage inner class fields accessible")
    void testUsageFields() {
        final var usage = new OpenAiResponseDto.Usage();
        usage.promptTokens = 10;
        usage.completionTokens = 20;
        usage.totalTokens = 30;
        assertEquals(10, usage.promptTokens.intValue());
        assertEquals(20, usage.completionTokens.intValue());
        assertEquals(30, usage.totalTokens.intValue());
    }

    @Test
    @DisplayName("Choice and Message fields accessible")
    void testChoiceMessageFields() {
        final var message = new OpenAiResponseDto.Message();
        message.role = "user";
        message.content = "What is 2+2?";
        assertEquals("user", message.role);
        assertEquals("What is 2+2?", message.content);

        final var choice = new OpenAiResponseDto.Choice();
        choice.index = 0;
        choice.message = message;
        choice.finishReason = "stop";
        assertEquals(0, choice.index.intValue());
        assertEquals("stop", choice.finishReason);
    }
}
