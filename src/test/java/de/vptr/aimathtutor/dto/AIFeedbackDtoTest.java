package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AIFeedbackDtoTest {

    private AiFeedbackDto feedbackDto;

    @BeforeEach
    void setUp() {
        this.feedbackDto = new AiFeedbackDto();
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
        final AiFeedbackDto dto = new AiFeedbackDto(
                AiFeedbackDto.FeedbackType.POSITIVE,
                "Great job!");

        // Then
        assertEquals(AiFeedbackDto.FeedbackType.POSITIVE, dto.type);
        assertEquals("Great job!", dto.message);
        assertNotNull(dto.timestamp);
        assertEquals(1.0, dto.confidence);
    }

    @Test
    @DisplayName("Should create positive feedback using factory method")
    void shouldCreatePositiveFeedbackUsingFactoryMethod() {
        // When
        final AiFeedbackDto dto = AiFeedbackDto.positive("Excellent work!");

        // Then
        assertEquals(AiFeedbackDto.FeedbackType.POSITIVE, dto.type);
        assertEquals("Excellent work!", dto.message);
    }

    @Test
    @DisplayName("Should create corrective feedback using factory method")
    void shouldCreateCorrectiveFeedbackUsingFactoryMethod() {
        // When
        final AiFeedbackDto dto = AiFeedbackDto.corrective("That's not quite right.");

        // Then
        assertEquals(AiFeedbackDto.FeedbackType.CORRECTIVE, dto.type);
        assertEquals("That's not quite right.", dto.message);
    }

    @Test
    @DisplayName("Should create hint feedback using factory method")
    void shouldCreateHintFeedbackUsingFactoryMethod() {
        // When
        final AiFeedbackDto dto = AiFeedbackDto.hint("Try factoring first.");

        // Then
        assertEquals(AiFeedbackDto.FeedbackType.HINT, dto.type);
        assertEquals("Try factoring first.", dto.message);
    }

    @Test
    @DisplayName("Should create suggestion feedback using factory method")
    void shouldCreateSuggestionFeedbackUsingFactoryMethod() {
        // When
        final AiFeedbackDto dto = AiFeedbackDto.suggestion("Consider a different approach.");

        // Then
        assertEquals(AiFeedbackDto.FeedbackType.SUGGESTION, dto.type);
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
        this.feedbackDto.type = AiFeedbackDto.FeedbackType.HINT;
        this.feedbackDto.message = "Try this approach";
        this.feedbackDto.detailedExplanation = "Here's why...";
        this.feedbackDto.confidence = 0.95;
        this.feedbackDto.sessionId = "session-123";

        // Then
        assertEquals(AiFeedbackDto.FeedbackType.HINT, this.feedbackDto.type);
        assertEquals("Try this approach", this.feedbackDto.message);
        assertEquals("Here's why...", this.feedbackDto.detailedExplanation);
        assertEquals(0.95, this.feedbackDto.confidence);
        assertEquals("session-123", this.feedbackDto.sessionId);
    }

    @Test
    @DisplayName("Should generate toString with all fields")
    void shouldGenerateToStringWithAllFields() {
        // Given
        this.feedbackDto.type = AiFeedbackDto.FeedbackType.POSITIVE;
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
        assertEquals(5, AiFeedbackDto.FeedbackType.values().length);
        assertNotNull(AiFeedbackDto.FeedbackType.POSITIVE);
        assertNotNull(AiFeedbackDto.FeedbackType.CORRECTIVE);
        assertNotNull(AiFeedbackDto.FeedbackType.HINT);
        assertNotNull(AiFeedbackDto.FeedbackType.SUGGESTION);
        assertNotNull(AiFeedbackDto.FeedbackType.NEUTRAL);
    }
}
