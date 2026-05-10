package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.AiFeedbackDto.FeedbackType;

@SuppressWarnings("NullAway")
class AiFeedbackDtoTest {

    @Test
    @DisplayName("Default constructor initializes defaults")
    void testDefaultConstructor() {
        final var dto = new AiFeedbackDto();
        assertEquals(FeedbackType.NEUTRAL, dto.type);
        assertEquals("", dto.message);
        assertEquals("", dto.detailedExplanation);
        assertNotNull(dto.hints);
        assertTrue(dto.hints.isEmpty());
        assertNotNull(dto.suggestedNextSteps);
        assertTrue(dto.suggestedNextSteps.isEmpty());
        assertNotNull(dto.relatedConcepts);
        assertTrue(dto.relatedConcepts.isEmpty());
        assertEquals(0.5, dto.confidence);
        assertNotNull(dto.timestamp);
        assertEquals("", dto.sessionId);
    }

    @Test
    @DisplayName("Parameterized constructor sets type and message")
    void testParameterizedConstructor() {
        final var dto = new AiFeedbackDto(FeedbackType.POSITIVE, "Good job!");
        assertEquals(FeedbackType.POSITIVE, dto.type);
        assertEquals("Good job!", dto.message);
    }

    @Test
    @DisplayName("Parameterized constructor handles null message")
    void testParameterizedConstructorNullMessage() {
        final var dto = new AiFeedbackDto(FeedbackType.CORRECTIVE, null);
        assertEquals(FeedbackType.CORRECTIVE, dto.type);
        assertEquals("", dto.message);
    }

    @Test
    @DisplayName("positive factory method")
    void testPositive() {
        final var dto = AiFeedbackDto.positive("Well done!");
        assertEquals(FeedbackType.POSITIVE, dto.type);
        assertEquals("Well done!", dto.message);
    }

    @Test
    @DisplayName("corrective factory method")
    void testCorrective() {
        final var dto = AiFeedbackDto.corrective("Check your signs");
        assertEquals(FeedbackType.CORRECTIVE, dto.type);
    }

    @Test
    @DisplayName("hint factory method")
    void testHint() {
        final var dto = AiFeedbackDto.hint("Try factoring first");
        assertEquals(FeedbackType.HINT, dto.type);
    }

    @Test
    @DisplayName("suggestion factory method")
    void testSuggestion() {
        final var dto = AiFeedbackDto.suggestion("Use distributive property");
        assertEquals(FeedbackType.SUGGESTION, dto.type);
    }

    @Test
    @DisplayName("neutral factory method")
    void testNeutral() {
        final var dto = AiFeedbackDto.neutral("Problem loaded");
        assertEquals(FeedbackType.NEUTRAL, dto.type);
    }

    @Test
    @DisplayName("error factory method creates corrective")
    void testError() {
        final var dto = AiFeedbackDto.error("Invalid operation");
        assertEquals(FeedbackType.CORRECTIVE, dto.type);
    }

    @Test
    @DisplayName("clampConfidence clamps to range")
    void testClampConfidence() {
        final var dto = new AiFeedbackDto();
        dto.confidence = 1.5;
        dto.clampConfidence();
        assertEquals(1.0, dto.confidence);

        dto.confidence = -0.3;
        dto.clampConfidence();
        assertEquals(0.0, dto.confidence);

        dto.confidence = 0.7;
        dto.clampConfidence();
        assertEquals(0.7, dto.confidence);
    }

    @Test
    @DisplayName("clampConfidence handles null")
    void testClampConfidenceNull() {
        final var dto = new AiFeedbackDto();
        dto.confidence = null;
        dto.clampConfidence();
        assertNull(dto.confidence);
    }

    @Test
    @DisplayName("toString returns summary")
    void testToString() {
        final var dto = new AiFeedbackDto(FeedbackType.POSITIVE, "Great!");
        dto.confidence = 0.9;
        final var str = dto.toString();
        assertTrue(str.contains("AIFeedbackDto"));
        assertTrue(str.contains("POSITIVE"));
        assertTrue(str.contains("Great!"));
    }
}
