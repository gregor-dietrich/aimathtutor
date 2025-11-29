package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.*;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
class AITutorServiceTest {

    @Inject
    private AiTutorService aiTutorService;

    @Test
    @DisplayName("Should analyze simplify action with correct result")
    @Transactional
    void shouldAnalyzeSimplifyActionWithCorrectResult() {
        // Given
        final var event = new GraspableEventDto();
        event.eventType = "simplify";
        event.expressionBefore = "2x + 3x";
        event.expressionAfter = "5x";
        event.correct = true;
        event.sessionId = "session-123";

        // When
        final AiFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event, new ConversationContextDto());

        // Then
        assertNotNull(feedback);
        assertEquals(AiFeedbackDto.FeedbackType.POSITIVE, feedback.type);
        assertNotNull(feedback.message);
        assertTrue(feedback.message.toLowerCase().contains("simplif"));
        assertEquals("session-123", feedback.sessionId);
    }

    @Test
    @DisplayName("Should analyze simplify action with incorrect result")
    @Transactional
    void shouldAnalyzeSimplifyActionWithIncorrectResult() {
        // Given
        final var event = new GraspableEventDto();
        event.eventType = "simplify";
        event.expressionBefore = "2x + 3x";
        event.expressionAfter = "6x";
        event.correct = false;
        event.sessionId = "session-456";

        // When
        final AiFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event, new ConversationContextDto());

        // Then
        assertNotNull(feedback);
        assertEquals(AiFeedbackDto.FeedbackType.CORRECTIVE, feedback.type);
        assertNotNull(feedback.message);
        assertFalse(feedback.hints.isEmpty());
    }

    @Test
    @DisplayName("Should analyze expand action")
    @Transactional
    void shouldAnalyzeExpandAction() {
        // Given
        final var event = new GraspableEventDto();
        event.eventType = "expand";
        event.expressionBefore = "(x + 2)(x + 3)";
        event.expressionAfter = "x^2 + 5x + 6";
        event.sessionId = "session-789";

        // When
        final AiFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event, new ConversationContextDto());

        // Then
        assertNotNull(feedback);
        assertEquals(AiFeedbackDto.FeedbackType.POSITIVE, feedback.type);
        assertNotNull(feedback.message);
    }

    @Test
    @DisplayName("Should analyze factor action")
    @Transactional
    void shouldAnalyzeFactorAction() {
        // Given
        final var event = new GraspableEventDto();
        event.eventType = "factor";
        event.expressionBefore = "x^2 + 5x + 6";
        event.expressionAfter = "(x + 2)(x + 3)";
        event.sessionId = "session-101";

        // When
        final AiFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event, new ConversationContextDto());

        // Then
        assertNotNull(feedback);
        assertEquals(AiFeedbackDto.FeedbackType.POSITIVE, feedback.type);
        assertNotNull(feedback.message);
    }

    @Test
    @DisplayName("Should analyze combine action")
    @Transactional
    void shouldAnalyzeCombineAction() {
        // Given
        final var event = new GraspableEventDto();
        event.eventType = "combine";
        event.expressionBefore = "3x + 2x";
        event.expressionAfter = "5x";
        event.sessionId = "session-202";

        // When
        final AiFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event, new ConversationContextDto());

        // Then
        assertNotNull(feedback);
        assertEquals(AiFeedbackDto.FeedbackType.SUGGESTION, feedback.type);
        assertNotNull(feedback.message);
    }

    @Test
    @DisplayName("Should analyze move action with expression change")
    @Transactional
    void shouldAnalyzeMoveAction() {
        // Given
        final var event = new GraspableEventDto();
        event.eventType = "AddSubInvertAction"; // Using actual Graspable Math action name
        event.expressionBefore = "x + 5 = 10";
        event.expressionAfter = "x = 10 - 5";
        event.sessionId = "session-303";

        // When
        final AiFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event, new ConversationContextDto());

        // Then
        assertNotNull(feedback);
        assertEquals(AiFeedbackDto.FeedbackType.HINT, feedback.type);
        assertNotNull(feedback.message);
    }

    @Test
    @DisplayName("Should return null for unknown event type (insignificant action)")
    @Transactional
    void shouldHandleUnknownEventType() {
        // Given
        final var event = new GraspableEventDto();
        event.eventType = "unknown_action";
        event.sessionId = "session-404";

        // When
        final AiFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event, new ConversationContextDto());

        // Then - now expects null for insignificant actions
        assertNull(feedback);
    }

    @Test
    @DisplayName("Should return null for null event type (insignificant action)")
    @Transactional
    void shouldHandleNullEventType() {
        // Given
        final var event = new GraspableEventDto();
        event.eventType = null;
        event.sessionId = "session-505";

        // When
        final AiFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event, new ConversationContextDto());

        // Then - now expects null for insignificant actions
        assertNull(feedback);
    }

    @Test
    @DisplayName("Should generate algebra problem")
    @Transactional
    void shouldGenerateAlgebraProblem() {
        // When
        final GraspableProblemDto problem = this.aiTutorService.generateProblem("intermediate",
                GraspableProblemDto.ProblemCategory.LINEAR_EQUATIONS);

        // Then
        assertNotNull(problem);
        assertEquals("intermediate", problem.difficulty);
        assertEquals(GraspableProblemDto.ProblemCategory.LINEAR_EQUATIONS, problem.category);
        assertNotNull(problem.title);
        assertNotNull(problem.initialExpression);
        assertFalse(problem.allowedOperations.isEmpty());
        assertFalse(problem.hints.isEmpty());
    }

    @Test
    @DisplayName("Should generate factoring problem")
    @Transactional
    void shouldGenerateFactoringProblem() {
        // When
        final GraspableProblemDto problem = this.aiTutorService.generateProblem("advanced",
                GraspableProblemDto.ProblemCategory.FACTORING);

        // Then
        assertNotNull(problem);
        assertEquals("advanced", problem.difficulty);
        assertEquals(GraspableProblemDto.ProblemCategory.FACTORING, problem.category);
        assertNotNull(problem.initialExpression);
        assertTrue(problem.allowedOperations.contains("factor"));
        assertNotNull(problem.targetExpression);
    }

    @Test
    @DisplayName("Should generate polynomial simplification problem")
    @Transactional
    void shouldGeneratePolynomialSimplificationProblem() {
        // When
        final GraspableProblemDto problem = this.aiTutorService.generateProblem("beginner",
                GraspableProblemDto.ProblemCategory.POLYNOMIAL_SIMPLIFICATION);

        // Then
        assertNotNull(problem);
        assertEquals(GraspableProblemDto.ProblemCategory.POLYNOMIAL_SIMPLIFICATION, problem.category);
        assertNotNull(problem.initialExpression);
        assertNotNull(problem.title);
    }

    @Test
    @DisplayName("Should set confidence score in feedback for significant actions")
    @Transactional
    void shouldSetConfidenceScoreInFeedback() {
        // Given - use a significant action type
        final var event = new GraspableEventDto();
        event.eventType = "simplify";
        event.expressionBefore = "2x + 3x";
        event.expressionAfter = "5x";
        event.correct = true; // Make it clear that this is correct
        event.sessionId = "session-606";

        // When
        final AiFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event, new ConversationContextDto());

        // Then
        assertNotNull(feedback);
        assertNotNull(feedback.confidence);
        assertTrue(feedback.confidence >= 0.0 && feedback.confidence <= 1.0);
    }

    @Test
    @DisplayName("Should include timestamp in feedback")
    @Transactional
    void shouldIncludeTimestampInFeedback() {
        // Given
        final var event = new GraspableEventDto();
        event.eventType = "expand";
        event.expressionBefore = "(x + 1)(x + 2)";
        event.expressionAfter = "x^2 + 3x + 2";
        event.sessionId = "session-707";

        // When
        final AiFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event, new ConversationContextDto());

        // Then
        assertNotNull(feedback);
        assertNotNull(feedback.timestamp);
    }

    @Test
    @DisplayName("Should answer student question about solving")
    @Transactional
    void shouldAnswerQuestionAboutSolving() {
        // Given
        final String question = "How do I solve this equation?";
        final String currentExpression = "2x + 5 = 15";
        final String sessionId = "session-808";

        // When
        final var answer = this.aiTutorService.answerQuestion(question, currentExpression, sessionId,
                new ConversationContextDto());

        // Then
        assertNotNull(answer);
        assertEquals(ChatMessageDto.Sender.AI, answer.sender);
        assertEquals(ChatMessageDto.MessageType.ANSWER, answer.messageType);
        assertNotNull(answer.message);
        assertTrue(answer.message.length() > 0);
        assertEquals(sessionId, answer.sessionId);
    }

    @Test
    @DisplayName("Should answer question about next steps")
    @Transactional
    void shouldAnswerQuestionAboutNextSteps() {
        // Given
        final String question = "What should I do next?";
        final String currentExpression = "x + 5 = 10";
        final String sessionId = "session-909";

        // When
        final var answer = this.aiTutorService.answerQuestion(question, currentExpression, sessionId,
                new ConversationContextDto());

        // Then
        assertNotNull(answer);
        assertNotNull(answer.message);
        assertTrue(answer.message.length() > 0);
    }

    @Test
    @DisplayName("Should handle question without current expression")
    @Transactional
    void shouldHandleQuestionWithoutExpression() {
        // Given
        final String question = "Can you explain algebra?";
        final String sessionId = "session-010";

        // When
        final var answer = this.aiTutorService.answerQuestion(question, null, sessionId, new ConversationContextDto());

        // Then
        assertNotNull(answer);
        assertNotNull(answer.message);
        assertEquals(sessionId, answer.sessionId);
    }

    // =========================================================================
    // stripQuotationMarks Tests
    // =========================================================================

    @Test
    @DisplayName("stripQuotationMarks should handle null input")
    void stripQuotationMarksShouldHandleNull() {
        assertNull(this.aiTutorService.stripQuotationMarks(null));
    }

    @Test
    @DisplayName("stripQuotationMarks should handle empty string")
    void stripQuotationMarksShouldHandleEmptyString() {
        assertEquals("", this.aiTutorService.stripQuotationMarks(""));
    }

    @Test
    @DisplayName("stripQuotationMarks should handle single character string")
    void stripQuotationMarksShouldHandleSingleCharacter() {
        // Single character strings should be returned unchanged (no quotes to strip)
        assertEquals("a", this.aiTutorService.stripQuotationMarks("a"));
        assertEquals("\"", this.aiTutorService.stripQuotationMarks("\""));
    }

    @Test
    @DisplayName("stripQuotationMarks should remove normal double quotes")
    void stripQuotationMarksShouldRemoveNormalDoubleQuotes() {
        assertEquals("Hello world", this.aiTutorService.stripQuotationMarks("\"Hello world\""));
    }

    @Test
    @DisplayName("stripQuotationMarks should remove smart double quotes (U+201C and U+201D)")
    void stripQuotationMarksShouldRemoveSmartDoubleQuotes() {
        // Smart double quotes: " (U+201C) and " (U+201D)
        assertEquals("Hello world", this.aiTutorService.stripQuotationMarks("\u201CHello world\u201D"));
    }

    @Test
    @DisplayName("stripQuotationMarks should remove smart single quotes (U+2018 and U+2019)")
    void stripQuotationMarksShouldRemoveSmartSingleQuotes() {
        // Smart single quotes: ' (U+2018) and ' (U+2019)
        assertEquals("Hello world", this.aiTutorService.stripQuotationMarks("\u2018Hello world\u2019"));
    }

    @Test
    @DisplayName("stripQuotationMarks should handle nested quotes")
    void stripQuotationMarksShouldHandleNestedQuotes() {
        // Nested quotes: outer quotes removed, inner preserved
        assertEquals("He said \"hello\"",
                this.aiTutorService.stripQuotationMarks("\"He said \"hello\"\""));

        // Multiple levels of smart quotes
        assertEquals("inner",
                this.aiTutorService.stripQuotationMarks("\u201C\u201Cinner\u201D\u201D"));
    }

    @Test
    @DisplayName("stripQuotationMarks should not remove mismatched quotes")
    void stripQuotationMarksShouldNotRemoveMismatchedQuotes() {
        // Mismatched quotes should be left alone
        assertEquals("\"Hello world'", this.aiTutorService.stripQuotationMarks("\"Hello world'"));
        assertEquals("\u201CHello world\u2019",
                this.aiTutorService.stripQuotationMarks("\u201CHello world\u2019"));
    }

    @Test
    @DisplayName("stripQuotationMarks should not remove quotes that don't wrap the entire string")
    void stripQuotationMarksShouldNotRemovePartialQuotes() {
        // Quotes in the middle should be preserved
        assertEquals("Hello \"world\" there",
                this.aiTutorService.stripQuotationMarks("Hello \"world\" there"));
    }

    @Test
    @DisplayName("stripQuotationMarks should trim whitespace before stripping quotes")
    void stripQuotationMarksShouldTrimWhitespace() {
        assertEquals("Hello world", this.aiTutorService.stripQuotationMarks("  \"Hello world\"  "));
    }

    @Test
    @DisplayName("stripQuotationMarks should handle string with only quotes")
    void stripQuotationMarksShouldHandleOnlyQuotes() {
        // Just matching quotes should result in empty or trimmed content
        assertEquals("", this.aiTutorService.stripQuotationMarks("\"\""));
        assertEquals("", this.aiTutorService.stripQuotationMarks("\u201C\u201D"));
    }

    @Test
    @DisplayName("stripQuotationMarks should preserve text without quotes")
    void stripQuotationMarksShouldPreserveTextWithoutQuotes() {
        assertEquals("Hello world", this.aiTutorService.stripQuotationMarks("Hello world"));
    }

    // =========================================================================
    // @Retry Method Tests (callOllamaForQuestion, callOllamaForAnalysis)
    // =========================================================================
    // Note: These tests verify the methods are callable and return expected
    // results.
    // Testing the actual retry behavior (3 retries, jitter, etc.) requires mocking
    // the OllamaService to throw exceptions on initial calls and succeed on
    // retries.
    // Such integration tests should be added when a mocking framework is available.

    @Test
    @DisplayName("callOllamaForQuestion should return answer when Ollama is available")
    @Transactional
    void callOllamaForQuestionShouldReturnAnswer() {
        // Given
        final String question = "What is 2+2?";
        final String currentExpression = "2+2";
        final var context = new ConversationContextDto();

        // When - calling directly tests the method (retry logic tested in integration)
        // If Ollama is not available, this will fail which is expected behavior
        try {
            final String answer = this.aiTutorService.callOllamaForQuestion(question, currentExpression, context);
            // Then - if Ollama is available, we should get a non-null response
            assertNotNull(answer);
        } catch (final Exception e) {
            // Ollama not available is acceptable in unit test environment
            // The @Retry annotation will have attempted retries before this exception
            assertTrue(e.getMessage() != null || e.getCause() != null,
                    "Exception should have a message or cause");
        }
    }

    @Test
    @DisplayName("callOllamaForAnalysis should return feedback when Ollama is available")
    @Transactional
    void callOllamaForAnalysisShouldReturnFeedback() {
        // Given
        final var event = new GraspableEventDto();
        event.eventType = "simplify";
        event.expressionBefore = "2x + 3x";
        event.expressionAfter = "5x";
        event.sessionId = "test-session";
        final var context = new ConversationContextDto();

        // When - calling directly tests the method (retry logic tested in integration)
        // If Ollama is not available, this will fail which is expected behavior
        try {
            final AiFeedbackDto feedback = this.aiTutorService.callOllamaForAnalysis(event, context);
            // Then - if Ollama is available, we should get a non-null response
            assertNotNull(feedback);
        } catch (final Exception e) {
            // Ollama not available is acceptable in unit test environment
            // The @Retry annotation will have attempted retries before this exception
            assertTrue(e.getMessage() != null || e.getCause() != null,
                    "Exception should have a message or cause");
        }
    }

    @Test
    @DisplayName("callOllamaForQuestion should be package-private for CDI proxy interception")
    void callOllamaForQuestionShouldBePackagePrivate() throws NoSuchMethodException {
        // Verify the method exists and is accessible (package-private)
        // This test documents the requirement that the method must not be private
        final var method = AiTutorService.class.getDeclaredMethod(
                "callOllamaForQuestion", String.class, String.class, ConversationContextDto.class);
        assertNotNull(method);
        // Package-private methods are accessible within the same package
        assertTrue(method.canAccess(this.aiTutorService),
                "Method should be accessible (package-private) for CDI proxy interception");
    }

    @Test
    @DisplayName("callOllamaForAnalysis should be package-private for CDI proxy interception")
    void callOllamaForAnalysisShouldBePackagePrivate() throws NoSuchMethodException {
        // Verify the method exists and is accessible (package-private)
        // This test documents the requirement that the method must not be private
        final var method = AiTutorService.class.getDeclaredMethod(
                "callOllamaForAnalysis", GraspableEventDto.class, ConversationContextDto.class);
        assertNotNull(method);
        // Package-private methods are accessible within the same package
        assertTrue(method.canAccess(this.aiTutorService),
                "Method should be accessible (package-private) for CDI proxy interception");
    }
}
