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
        assertNotNull(feedback.message);
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
        assertNotNull(feedback.message);
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
        assertNotNull(feedback.message);
    }

    @Test
    @DisplayName("Should analyze move action with expression change")
    void shouldAnalyzeMoveAction() {
        // Given
        final var event = new GraspableEventDto();
        event.eventType = "AddSubInvertAction"; // Using actual Graspable Math action name
        event.expressionBefore = "x + 5 = 10";
        event.expressionAfter = "x = 10 - 5";
        event.sessionId = "session-303";

        // When
        final AIFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event);

        // Then
        assertNotNull(feedback);
        assertEquals(AIFeedbackDto.FeedbackType.HINT, feedback.type);
        assertNotNull(feedback.message);
    }

    @Test
    @DisplayName("Should return null for unknown event type (insignificant action)")
    void shouldHandleUnknownEventType() {
        // Given
        final var event = new GraspableEventDto();
        event.eventType = "unknown_action";
        event.sessionId = "session-404";

        // When
        final AIFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event);

        // Then - now expects null for insignificant actions
        assertNull(feedback);
    }

    @Test
    @DisplayName("Should return null for null event type (insignificant action)")
    void shouldHandleNullEventType() {
        // Given
        final var event = new GraspableEventDto();
        event.eventType = null;
        event.sessionId = "session-505";

        // When
        final AIFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event);

        // Then - now expects null for insignificant actions
        assertNull(feedback);
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
    @DisplayName("Should set confidence score in feedback for significant actions")
    void shouldSetConfidenceScoreInFeedback() {
        // Given - use a significant action type
        final var event = new GraspableEventDto();
        event.eventType = "simplify";
        event.expressionBefore = "2x + 3x";
        event.expressionAfter = "5x";
        event.correct = true; // Make it clear that this is correct
        event.sessionId = "session-606";

        // When
        final AIFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event);

        // Then
        assertNotNull(feedback);
        assertNotNull(feedback.confidence);
        assertTrue(feedback.confidence >= 0.0 && feedback.confidence <= 1.0);
    }

    @Test
    @DisplayName("Should include timestamp in feedback")
    void shouldIncludeTimestampInFeedback() {
        // Given
        final var event = new GraspableEventDto();
        event.eventType = "expand";
        event.expressionBefore = "(x + 1)(x + 2)";
        event.expressionAfter = "x^2 + 3x + 2";
        event.sessionId = "session-707";

        // When
        final AIFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event);

        // Then
        assertNotNull(feedback);
        assertNotNull(feedback.timestamp);
    }

    @Test
    @DisplayName("Should answer student question about solving")
    void shouldAnswerQuestionAboutSolving() {
        // Given
        final String question = "How do I solve this equation?";
        final String currentExpression = "2x + 5 = 15";
        final String sessionId = "session-808";

        // When
        final var answer = this.aiTutorService.answerQuestion(question, currentExpression, sessionId);

        // Then
        assertNotNull(answer);
        assertEquals(de.vptr.aimathtutor.dto.ChatMessageDto.Sender.AI, answer.sender);
        assertEquals(de.vptr.aimathtutor.dto.ChatMessageDto.MessageType.ANSWER, answer.messageType);
        assertNotNull(answer.message);
        assertTrue(answer.message.length() > 0);
        assertEquals(sessionId, answer.sessionId);
    }

    @Test
    @DisplayName("Should answer question about next steps")
    void shouldAnswerQuestionAboutNextSteps() {
        // Given
        final String question = "What should I do next?";
        final String currentExpression = "x + 5 = 10";
        final String sessionId = "session-909";

        // When
        final var answer = this.aiTutorService.answerQuestion(question, currentExpression, sessionId);

        // Then
        assertNotNull(answer);
        assertNotNull(answer.message);
        assertTrue(answer.message.length() > 0);
    }

    @Test
    @DisplayName("Should handle question without current expression")
    void shouldHandleQuestionWithoutExpression() {
        // Given
        final String question = "Can you explain algebra?";
        final String sessionId = "session-010";

        // When
        final var answer = this.aiTutorService.answerQuestion(question, null, sessionId);

        // Then
        assertNotNull(answer);
        assertNotNull(answer.message);
        assertEquals(sessionId, answer.sessionId);
    }
}
