package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.aimathtutor.dto.AIFeedbackDto;
import de.vptr.aimathtutor.dto.GraspableEventDto;
import de.vptr.aimathtutor.dto.GraspableProblemDto;

@ExtendWith(MockitoExtension.class)
class AITutorServiceTest {

    private AITutorService aiTutorService;

    @BeforeEach
    void setUp() {
        this.aiTutorService = new AITutorService();
        // Set test configuration values using reflection or test-specific setup
        // For now, the service will use default values
    }

    @Test
    @DisplayName("Should analyze simplify action with correct result")
    void shouldAnalyzeSimplifyActionWithCorrectResult() {
        // Given
        final var event = new GraspableEventDto();
        event.eventType = "simplify";
        event.expressionBefore = "2x + 3x";
        event.expressionAfter = "5x";
        event.correct = true;
        event.sessionId = "session-123";

        // When
        final AIFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event);

        // Then
        assertNotNull(feedback);
        assertEquals(AIFeedbackDto.FeedbackType.POSITIVE, feedback.type);
        assertNotNull(feedback.message);
        assertTrue(feedback.message.toLowerCase().contains("simplif"));
        assertEquals("session-123", feedback.sessionId);
    }

    @Test
    @DisplayName("Should analyze simplify action with incorrect result")
    void shouldAnalyzeSimplifyActionWithIncorrectResult() {
        // Given
        final var event = new GraspableEventDto();
        event.eventType = "simplify";
        event.expressionBefore = "2x + 3x";
        event.expressionAfter = "6x";
        event.correct = false;
        event.sessionId = "session-456";

        // When
        final AIFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event);

        // Then
        assertNotNull(feedback);
        assertEquals(AIFeedbackDto.FeedbackType.CORRECTIVE, feedback.type);
        assertNotNull(feedback.message);
        assertFalse(feedback.hints.isEmpty());
    }

    @Test
    @DisplayName("Should analyze expand action")
    void shouldAnalyzeExpandAction() {
        // Given
        final var event = new GraspableEventDto();
        event.eventType = "expand";
        event.expressionBefore = "(x + 2)(x + 3)";
        event.expressionAfter = "x^2 + 5x + 6";
        event.sessionId = "session-789";

        // When
        final AIFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event);

        // Then
        assertNotNull(feedback);
        assertEquals(AIFeedbackDto.FeedbackType.POSITIVE, feedback.type);
        assertTrue(feedback.relatedConcepts.contains("Distributive property"));
    }

    @Test
    @DisplayName("Should analyze factor action")
    void shouldAnalyzeFactorAction() {
        // Given
        final var event = new GraspableEventDto();
        event.eventType = "factor";
        event.expressionBefore = "x^2 + 5x + 6";
        event.expressionAfter = "(x + 2)(x + 3)";
        event.sessionId = "session-101";

        // When
        final AIFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event);

        // Then
        assertNotNull(feedback);
        assertEquals(AIFeedbackDto.FeedbackType.POSITIVE, feedback.type);
        assertFalse(feedback.relatedConcepts.isEmpty());
        assertTrue(feedback.relatedConcepts.stream()
                .anyMatch(c -> c.toLowerCase().contains("factor")));
    }

    @Test
    @DisplayName("Should analyze combine action")
    void shouldAnalyzeCombineAction() {
        // Given
        final var event = new GraspableEventDto();
        event.eventType = "combine";
        event.expressionBefore = "3x + 2x";
        event.expressionAfter = "5x";
        event.sessionId = "session-202";

        // When
        final AIFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event);

        // Then
        assertNotNull(feedback);
        assertEquals(AIFeedbackDto.FeedbackType.SUGGESTION, feedback.type);
        assertFalse(feedback.hints.isEmpty());
    }

    @Test
    @DisplayName("Should analyze move action")
    void shouldAnalyzeMoveAction() {
        // Given
        final var event = new GraspableEventDto();
        event.eventType = "move";
        event.expressionBefore = "x + 5 = 10";
        event.expressionAfter = "x = 10 - 5";
        event.sessionId = "session-303";

        // When
        final AIFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event);

        // Then
        assertNotNull(feedback);
        assertEquals(AIFeedbackDto.FeedbackType.HINT, feedback.type);
        assertFalse(feedback.relatedConcepts.isEmpty());
    }

    @Test
    @DisplayName("Should handle unknown event type")
    void shouldHandleUnknownEventType() {
        // Given
        final var event = new GraspableEventDto();
        event.eventType = "unknown_action";
        event.sessionId = "session-404";

        // When
        final AIFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event);

        // Then
        assertNotNull(feedback);
        assertNotNull(feedback.message);
        assertEquals("session-404", feedback.sessionId);
    }

    @Test
    @DisplayName("Should handle null event type")
    void shouldHandleNullEventType() {
        // Given
        final var event = new GraspableEventDto();
        event.eventType = null;
        event.sessionId = "session-505";

        // When
        final AIFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event);

        // Then
        assertNotNull(feedback);
        assertNotNull(feedback.message);
    }

    @Test
    @DisplayName("Should generate algebra problem")
    void shouldGenerateAlgebraProblem() {
        // When
        final GraspableProblemDto problem = this.aiTutorService.generateProblem("intermediate", "algebra");

        // Then
        assertNotNull(problem);
        assertEquals("intermediate", problem.difficulty);
        assertNotNull(problem.title);
        assertNotNull(problem.initialExpression);
        assertFalse(problem.allowedOperations.isEmpty());
        assertFalse(problem.hints.isEmpty());
    }

    @Test
    @DisplayName("Should generate factoring problem")
    void shouldGenerateFactoringProblem() {
        // When
        final GraspableProblemDto problem = this.aiTutorService.generateProblem("advanced", "factoring");

        // Then
        assertNotNull(problem);
        assertEquals("advanced", problem.difficulty);
        assertNotNull(problem.initialExpression);
        assertTrue(problem.allowedOperations.contains("factor"));
        assertNotNull(problem.targetExpression);
    }

    @Test
    @DisplayName("Should generate default problem for unknown topic")
    void shouldGenerateDefaultProblemForUnknownTopic() {
        // When
        final GraspableProblemDto problem = this.aiTutorService.generateProblem("beginner", "unknown_topic");

        // Then
        assertNotNull(problem);
        assertNotNull(problem.initialExpression);
        assertNotNull(problem.title);
    }

    @Test
    @DisplayName("Should set confidence score in feedback")
    void shouldSetConfidenceScoreInFeedback() {
        // Given
        final var event = new GraspableEventDto();
        event.eventType = "simplify";
        event.sessionId = "session-606";

        // When
        final AIFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event);

        // Then
        assertNotNull(feedback.confidence);
        assertTrue(feedback.confidence >= 0.0 && feedback.confidence <= 1.0);
    }

    @Test
    @DisplayName("Should include timestamp in feedback")
    void shouldIncludeTimestampInFeedback() {
        // Given
        final var event = new GraspableEventDto();
        event.eventType = "expand";
        event.sessionId = "session-707";

        // When
        final AIFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event);

        // Then
        assertNotNull(feedback.timestamp);
    }
}
