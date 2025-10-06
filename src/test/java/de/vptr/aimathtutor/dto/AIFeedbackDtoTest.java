package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AIFeedbackDtoTest {

    private AIFeedbackDto feedbackDto;

    @BeforeEach
    void setUp() {
        this.feedbackDto = new AIFeedbackDto();
    }

    @Test
    @DisplayName("Should create AIFeedbackDto with default constructor")
    void shouldCreateAIFeedbackDtoWithDefaultConstructor() {
        // Then
        assertNotNull(this.feedbackDto.timestamp);
        assertNotNull(this.feedbackDto.hints);
        assertNotNull(this.feedbackDto.suggestedNextSteps);
        assertNotNull(this.feedbackDto.relatedConcepts);
        assertEquals(1.0, this.feedbackDto.confidence);
        assertTrue(this.feedbackDto.hints.isEmpty());
        assertTrue(this.feedbackDto.suggestedNextSteps.isEmpty());
        assertTrue(this.feedbackDto.relatedConcepts.isEmpty());
    }

    @Test
    @DisplayName("Should create AIFeedbackDto with parameterized constructor")
    void shouldCreateAIFeedbackDtoWithParameterizedConstructor() {
        // When
        final AIFeedbackDto dto = new AIFeedbackDto(
                AIFeedbackDto.FeedbackType.POSITIVE,
                "Great job!");

        // Then
        assertEquals(AIFeedbackDto.FeedbackType.POSITIVE, dto.type);
        assertEquals("Great job!", dto.message);
        assertNotNull(dto.timestamp);
        assertEquals(1.0, dto.confidence);
    }

    @Test
    @DisplayName("Should create positive feedback using factory method")
    void shouldCreatePositiveFeedbackUsingFactoryMethod() {
        // When
        final AIFeedbackDto dto = AIFeedbackDto.positive("Excellent work!");

        // Then
        assertEquals(AIFeedbackDto.FeedbackType.POSITIVE, dto.type);
        assertEquals("Excellent work!", dto.message);
    }

    @Test
    @DisplayName("Should create corrective feedback using factory method")
    void shouldCreateCorrectiveFeedbackUsingFactoryMethod() {
        // When
        final AIFeedbackDto dto = AIFeedbackDto.corrective("That's not quite right.");

        // Then
        assertEquals(AIFeedbackDto.FeedbackType.CORRECTIVE, dto.type);
        assertEquals("That's not quite right.", dto.message);
    }

    @Test
    @DisplayName("Should create hint feedback using factory method")
    void shouldCreateHintFeedbackUsingFactoryMethod() {
        // When
        final AIFeedbackDto dto = AIFeedbackDto.hint("Try factoring first.");

        // Then
        assertEquals(AIFeedbackDto.FeedbackType.HINT, dto.type);
        assertEquals("Try factoring first.", dto.message);
    }

    @Test
    @DisplayName("Should create suggestion feedback using factory method")
    void shouldCreateSuggestionFeedbackUsingFactoryMethod() {
        // When
        final AIFeedbackDto dto = AIFeedbackDto.suggestion("Consider a different approach.");

        // Then
        assertEquals(AIFeedbackDto.FeedbackType.SUGGESTION, dto.type);
        assertEquals("Consider a different approach.", dto.message);
    }

    @Test
    @DisplayName("Should add hints to feedback")
    void shouldAddHintsToFeedback() {
        // When
        this.feedbackDto.hints.add("Hint 1");
        this.feedbackDto.hints.add("Hint 2");

        // Then
        assertEquals(2, this.feedbackDto.hints.size());
        assertTrue(this.feedbackDto.hints.contains("Hint 1"));
        assertTrue(this.feedbackDto.hints.contains("Hint 2"));
    }

    @Test
    @DisplayName("Should add suggested next steps")
    void shouldAddSuggestedNextSteps() {
        // When
        this.feedbackDto.suggestedNextSteps.add("Step 1");
        this.feedbackDto.suggestedNextSteps.add("Step 2");

        // Then
        assertEquals(2, this.feedbackDto.suggestedNextSteps.size());
        assertTrue(this.feedbackDto.suggestedNextSteps.contains("Step 1"));
    }

    @Test
    @DisplayName("Should add related concepts")
    void shouldAddRelatedConcepts() {
        // When
        this.feedbackDto.relatedConcepts.add("Algebra");
        this.feedbackDto.relatedConcepts.add("Factoring");

        // Then
        assertEquals(2, this.feedbackDto.relatedConcepts.size());
        assertTrue(this.feedbackDto.relatedConcepts.contains("Algebra"));
        assertTrue(this.feedbackDto.relatedConcepts.contains("Factoring"));
    }

    @Test
    @DisplayName("Should set all fields correctly")
    void shouldSetAllFieldsCorrectly() {
        // When
        this.feedbackDto.type = AIFeedbackDto.FeedbackType.HINT;
        this.feedbackDto.message = "Try this approach";
        this.feedbackDto.detailedExplanation = "Here's why...";
        this.feedbackDto.confidence = 0.95;
        this.feedbackDto.sessionId = "session-123";

        // Then
        assertEquals(AIFeedbackDto.FeedbackType.HINT, this.feedbackDto.type);
        assertEquals("Try this approach", this.feedbackDto.message);
        assertEquals("Here's why...", this.feedbackDto.detailedExplanation);
        assertEquals(0.95, this.feedbackDto.confidence);
        assertEquals("session-123", this.feedbackDto.sessionId);
    }

    @Test
    @DisplayName("Should generate toString with all fields")
    void shouldGenerateToStringWithAllFields() {
        // Given
        this.feedbackDto.type = AIFeedbackDto.FeedbackType.POSITIVE;
        this.feedbackDto.message = "Well done!";
        this.feedbackDto.confidence = 0.88;
        this.feedbackDto.sessionId = "session-456";

        // When
        final String result = this.feedbackDto.toString();

        // Then
        assertNotNull(result);
        assertTrue(result.contains("POSITIVE"));
        assertTrue(result.contains("Well done!"));
        assertTrue(result.contains("0.88"));
        assertTrue(result.contains("session-456"));
    }

    @Test
    @DisplayName("Should test all feedback types")
    void shouldTestAllFeedbackTypes() {
        // Test all enum values
        assertEquals(5, AIFeedbackDto.FeedbackType.values().length);
        assertNotNull(AIFeedbackDto.FeedbackType.POSITIVE);
        assertNotNull(AIFeedbackDto.FeedbackType.CORRECTIVE);
        assertNotNull(AIFeedbackDto.FeedbackType.HINT);
        assertNotNull(AIFeedbackDto.FeedbackType.SUGGESTION);
        assertNotNull(AIFeedbackDto.FeedbackType.NEUTRAL);
    }
}
