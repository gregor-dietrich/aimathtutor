package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GoogleResponseDtoTest {

    @Test
    @DisplayName("Should return text content when no thought blocks are present")
    void shouldReturnTextContentWithoutThoughts() {
        final var dto = new GoogleResponseDto();
        final var candidate = new GoogleResponseDto.Candidate();
        candidate.content = new GoogleResponseDto.Content();
        final var part = new GoogleResponseDto.Part();
        part.text = "Hello, how can I help you?";
        candidate.content.parts = List.of(part);
        dto.candidates = List.of(candidate);

        assertEquals("Hello, how can I help you?", dto.getTextContent());
    }

    @Test
    @DisplayName("Should skip thought=true parts and return actual text content")
    void shouldSkipThoughtTrueParts() {
        final var dto = new GoogleResponseDto();
        final var candidate = new GoogleResponseDto.Candidate();
        candidate.content = new GoogleResponseDto.Content();
        final var thoughtPart = new GoogleResponseDto.Part();
        thoughtPart.text = "The student is asking about algebra. I should guide them...";
        thoughtPart.thought = true;
        final var textPart = new GoogleResponseDto.Part();
        textPart.text = "Let's solve this step by step.";
        textPart.thought = false;
        candidate.content.parts = List.of(thoughtPart, textPart);
        dto.candidates = List.of(candidate);

        assertEquals("Let's solve this step by step.", dto.getTextContent());
    }

    @Test
    @DisplayName("Should skip thought=true part even when text part has no explicit thought flag")
    void shouldSkipThoughtTrueAndReturnPartWithoutThoughtFlag() {
        final var dto = new GoogleResponseDto();
        final var candidate = new GoogleResponseDto.Candidate();
        candidate.content = new GoogleResponseDto.Content();
        final var thoughtPart = new GoogleResponseDto.Part();
        thoughtPart.text = "Internal reasoning about the problem...";
        thoughtPart.thought = true;
        final var textPart = new GoogleResponseDto.Part();
        textPart.text = "Here is the answer for the student.";
        candidate.content.parts = List.of(thoughtPart, textPart);
        dto.candidates = List.of(candidate);

        assertEquals("Here is the answer for the student.", dto.getTextContent());
    }

    @Test
    @DisplayName("Should handle multiple thought blocks before text")
    void shouldHandleMultipleThoughtBlocksBeforeText() {
        final var dto = new GoogleResponseDto();
        final var candidate = new GoogleResponseDto.Candidate();
        candidate.content = new GoogleResponseDto.Content();
        final var thought1 = new GoogleResponseDto.Part();
        thought1.text = "First thought about the problem.";
        thought1.thought = true;
        final var thought2 = new GoogleResponseDto.Part();
        thought2.text = "Second thought refining the approach.";
        thought2.thought = true;
        final var textPart = new GoogleResponseDto.Part();
        textPart.text = "Here is the answer.";
        candidate.content.parts = List.of(thought1, thought2, textPart);
        dto.candidates = List.of(candidate);

        assertEquals("Here is the answer.", dto.getTextContent());
    }

    @Test
    @DisplayName("Should return null when only thought blocks are present")
    void shouldReturnNullWhenOnlyThoughtsPresent() {
        final var dto = new GoogleResponseDto();
        final var candidate = new GoogleResponseDto.Candidate();
        candidate.content = new GoogleResponseDto.Content();
        final var thoughtPart = new GoogleResponseDto.Part();
        thoughtPart.text = "Only reasoning, no answer.";
        thoughtPart.thought = true;
        candidate.content.parts = List.of(thoughtPart);
        dto.candidates = List.of(candidate);

        assertNull(dto.getTextContent());
    }

    @Test
    @DisplayName("Should return text from part with thought=false")
    void shouldReturnTextFromPartWithThoughtFalse() {
        final var dto = new GoogleResponseDto();
        final var candidate = new GoogleResponseDto.Candidate();
        candidate.content = new GoogleResponseDto.Content();
        final var textPart = new GoogleResponseDto.Part();
        textPart.text = "This is the actual response.";
        textPart.thought = false;
        candidate.content.parts = List.of(textPart);
        dto.candidates = List.of(candidate);

        assertEquals("This is the actual response.", dto.getTextContent());
    }

    @Test
    @DisplayName("Should return null when candidates list is empty")
    void shouldReturnNullForEmptyCandidates() {
        final var dto = new GoogleResponseDto();
        dto.candidates = List.of();

        assertNull(dto.getTextContent());
    }

    @Test
    @DisplayName("Should return null when candidates is null")
    void shouldReturnNullForNullCandidates() {
        final var dto = new GoogleResponseDto();

        assertNull(dto.getTextContent());
    }

    @Test
    @DisplayName("Should detect blocked response")
    void shouldDetectBlockedResponse() {
        final var dto = new GoogleResponseDto();
        final var candidate = new GoogleResponseDto.Candidate();
        candidate.finishReason = "SAFETY";
        dto.candidates = List.of(candidate);

        assertTrue(dto.isBlocked());
    }

    @Test
    @DisplayName("Should detect truncated response")
    void shouldDetectTruncatedResponse() {
        final var dto = new GoogleResponseDto();
        final var candidate = new GoogleResponseDto.Candidate();
        candidate.finishReason = "MAX_TOKENS";
        dto.candidates = List.of(candidate);

        assertTrue(dto.isTruncated());
    }

    @Test
    @DisplayName("Should not detect blocked response for normal finish")
    void shouldNotDetectBlockedForNormalFinish() {
        final var dto = new GoogleResponseDto();
        final var candidate = new GoogleResponseDto.Candidate();
        candidate.finishReason = "STOP";
        dto.candidates = List.of(candidate);

        assertFalse(dto.isBlocked());
    }

    @Test
    @DisplayName("Should not detect truncated response for normal finish")
    void shouldNotDetectTruncatedForNormalFinish() {
        final var dto = new GoogleResponseDto();
        final var candidate = new GoogleResponseDto.Candidate();
        candidate.finishReason = "STOP";
        dto.candidates = List.of(candidate);

        assertFalse(dto.isTruncated());
    }

    @Test
    @DisplayName("Should return empty response flag when candidates is null")
    void shouldReturnEmptyForNullCandidates() {
        final var dto = new GoogleResponseDto();

        assertTrue(dto.isEmptyResponse());
    }

    @Test
    @DisplayName("Should return empty response flag when candidates is empty")
    void shouldReturnEmptyForEmptyCandidates() {
        final var dto = new GoogleResponseDto();
        dto.candidates = List.of();

        assertTrue(dto.isEmptyResponse());
    }

    @Test
    @DisplayName("Should return finish reason from first candidate")
    void shouldReturnFinishReason() {
        final var dto = new GoogleResponseDto();
        final var candidate = new GoogleResponseDto.Candidate();
        candidate.finishReason = "STOP";
        dto.candidates = List.of(candidate);

        assertEquals("STOP", dto.getFinishReason());
    }

    @Test
    @DisplayName("Should return null finish reason when candidates is null")
    void shouldReturnNullFinishReasonForNullCandidates() {
        final var dto = new GoogleResponseDto();

        assertNull(dto.getFinishReason());
    }

    @Test
    @DisplayName("Should skip null entries in parts list")
    void shouldSkipNullParts() {
        final var dto = new GoogleResponseDto();
        final var candidate = new GoogleResponseDto.Candidate();
        candidate.content = new GoogleResponseDto.Content();
        final var validPart = new GoogleResponseDto.Part();
        validPart.text = "Valid response";
        validPart.thought = false;
        candidate.content.parts = Arrays.asList(null, validPart);
        dto.candidates = List.of(candidate);

        assertEquals("Valid response", dto.getTextContent());
    }
}
