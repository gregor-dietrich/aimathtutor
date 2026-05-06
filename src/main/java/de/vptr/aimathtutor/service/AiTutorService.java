package de.vptr.aimathtutor.service;

import java.util.concurrent.CompletableFuture;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

import de.vptr.aimathtutor.dto.AiFeedbackDto;
import de.vptr.aimathtutor.dto.ChatMessageDto;
import de.vptr.aimathtutor.dto.ConversationContextDto;
import de.vptr.aimathtutor.dto.ExerciseDto.DifficultyLevel;
import de.vptr.aimathtutor.dto.GraspableEventDto;
import de.vptr.aimathtutor.dto.GraspableProblemDto;
import de.vptr.aimathtutor.service.ai.AiConfigKeys;
import de.vptr.aimathtutor.service.ai.AiInteractionLogger;
import de.vptr.aimathtutor.service.ai.JsonRepairService;
import de.vptr.aimathtutor.service.ai.provider.AiProvider;
import de.vptr.aimathtutor.service.ai.provider.GeminiAiProvider;
import de.vptr.aimathtutor.service.ai.provider.MockAiProvider;
import de.vptr.aimathtutor.service.ai.provider.OllamaAiProvider;
import de.vptr.aimathtutor.service.ai.provider.OpenAiProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * AI Tutor Service - Analyzes student math actions and provides feedback.
 *
 * Configuration is loaded dynamically from AiConfigService, including:
 * - Whether AI is enabled
 * - Which AI provider to use (mock, gemini, openai, ollama)
 * - Prompt templates for question answering and math tutoring
 *
 * This service acts as an orchestrator, delegating actual AI provider calls to
 * strategy implementations and logging to AiInteractionLogger.
 */
@ApplicationScoped
public class AiTutorService {

    private static final Logger LOG = Logger.getLogger(AiTutorService.class);

    @Inject
    AiConfigService aiConfigService;

    @Inject
    MockAiProvider mockAiProvider;

    @Inject
    GeminiAiProvider geminiAiProvider;

    @Inject
    OpenAiProvider openAiProvider;

    @Inject
    OllamaAiProvider ollamaAiProvider;

    @Inject
    ProblemGeneratorService problemGeneratorService;

    @Inject
    JsonRepairService jsonRepairService;

    @Inject
    AiInteractionLogger aiInteractionLogger;

    @Inject
    ManagedExecutor managedExecutor;

    @Inject
    RateLimitService rateLimitService;

    @Inject
    AuthService authService;

    /**
     * Analyzes a student's math action and provides AI feedback.
     * Only provides feedback for significant actions to reduce spam.
     *
     * Note: Not @Transactional because it calls long-running AI services.
     * DB operations are handled in separate transactional methods (logInteraction).
     *
     * @param event     The Graspable Math event containing the student's action
     * @param context   Conversation context with recent actions, questions, and AI
     *                  messages
     * @param userIdStr the user ID (as string) for rate limiting; if null, uses
     *                  event.studentId
     * @return AI-generated feedback, or null if no feedback needed
     */
    AiFeedbackDto analyzeMathAction(final GraspableEventDto event, final ConversationContextDto context,
            final String userIdStr) {
        if (event == null) {
            throw new IllegalArgumentException("event cannot be null");
        }
        LOG.infof("Analyzing math action: eventType='%s', beforeLen=%s, afterLen=%s, contextActions=%s", 
                event.eventType, 
                event.expressionBefore != null ? event.expressionBefore.length() : 0, 
                event.expressionAfter != null ? event.expressionAfter.length() : 0, 
                context != null ? context.getRecentActions().size() : 0);

        // Load dynamic configuration (null-safe)
        final Boolean aiEnabled = this.getConfigBoolean(AiConfigKeys.AI_TUTOR_ENABLED, true);
        if (!aiEnabled) {
            LOG.debug("AI is disabled, returning null");
            return null; // Don't provide feedback if AI is disabled
        }

        // Check if problem is completed
        if (event.isComplete != null && event.isComplete) {
            LOG.info("Problem completed! Generating congratulatory feedback.");
            return this.generateCompletionFeedback(event);
        }

        // Filter out insignificant actions to reduce spam
        if (!this.isSignificantAction(event)) {
            LOG.infof("Skipping feedback for insignificant action: eventType='%s'", 
                    event.eventType);
            return null;
        }

        // Load dynamic provider configuration
        final var aiProvider = this.getConfigString(AiConfigKeys.AI_TUTOR_PROVIDER, "mock");
        LOG.infof("Action is significant, generating feedback with provider: %s",  aiProvider);

        // Use different AI provider based on configuration
        final var provider = (aiProvider != null) ? aiProvider.toLowerCase() : "mock";

        // Apply per-user rate limiting for non-mock providers
        final var effectiveUserId = userIdStr != null ? userIdStr
                : (event.studentId != null ? String.valueOf(event.studentId) : null);
        if (!"mock".equals(provider) && !this.checkAiRateLimit(effectiveUserId)) {
            return AiFeedbackDto.hint("I'm receiving too many requests. Please wait a moment before your next action.");
        }

        return switch (provider) {
            case "gemini" -> this.safeAnalyze(this.geminiAiProvider, event, context);
            case "openai" -> this.safeAnalyze(this.openAiProvider, event, context);
            case "ollama" -> this.safeAnalyze(this.ollamaAiProvider, event, context);
            default -> this.mockAiProvider.analyzeMathAction(event, context);
        };
    }

    /**
     * Generates congratulatory feedback when a problem is completed.
     *
     * @param event The completion event
     * @return Congratulatory feedback message
     */
    private AiFeedbackDto generateCompletionFeedback(final GraspableEventDto event) {
        final String[] congratulatoryMessages = {
                "🎉 Excellent work! You've solved it correctly!",
                "🌟 Perfect! You reached the solution: " + event.expressionAfter,
                "👏 Outstanding! You've successfully completed the problem!",
                "✨ Great job! You've mastered this problem!",
                "🎯 Fantastic! You solved it: " + event.expressionAfter,
                "💯 Well done! You've reached the correct solution!",
                "🏆 Amazing work! Problem solved successfully!",
                "💪 Strong work! You crushed this problem!",
                "👍 Awesome! You got it right!",
                "🏅 Champion! You conquered this challenge!",
                "🎊 Congratulations! You nailed it!"
        };

        // Pick a random congratulatory message
        final int index = (int) (Math.random() * congratulatoryMessages.length);
        final var message = congratulatoryMessages[index];

        return AiFeedbackDto.positive(message);
    }

    /**
     * Checks per-user rate limiting for AI tutor calls.
     * Atomically checks and records the call if allowed.
     *
     * @param userIdStr the user ID as a string
     * @return true if the call is within the rate limit and was recorded
     */
    private boolean checkAiRateLimit(final String userIdStr) {
        if (userIdStr == null) {
            return false;
        }
        if (!this.rateLimitService.tryConsume(userIdStr)) {
            LOG.warnf("AI tutor rate limit exceeded for user: %s",  userIdStr);
            return false;
        }
        return true;
    }

    /**
     * Determines if an action is significant enough to warrant feedback.
     * Helps reduce spammy feedback on every minor change.
     */
    private boolean isSignificantAction(final GraspableEventDto event) {
        if (event.eventType == null) {
            return false;
        }

        final var type = event.eventType.toLowerCase();

        // Graspable Math specific action names (from the actual GM API)
        if (type.contains("addsubinvert") // Adding/subtracting to both sides
                || type.contains("muldivinvert") // Multiplying/dividing both sides
                || type.contains("fractioncancel") // Simplifying fractions
                || type.contains("distribute") // Distributing multiplication
                || type.contains("factor") // Factoring
                || type.contains("collect") // Collecting like terms
                || type.contains("commute") // Commutative property
                || type.contains("associate")) { // Associative property
            return true;
        }

        // Generic action keywords (might be used by other math tools or custom actions)
        if (type.contains("simplify")
                || type.contains("expand")
                || type.contains("solve")
                || type.contains("combine")
                || type.contains("isolate")
                || type.contains("substitute")) {
            return true;
        }

        // Skip automatic simplification actions to reduce noise
        if (type.contains("postinteraction")
                || type.contains("addsubnumbers") // Automatic number combining
                || type.contains("autosimp")) {
            LOG.debugf("Skipping automatic simplification: %s",  type);
            return false;
        }

        // For generic "change" or "math_step" events, check if expression actually
        // changed
        if ((type.contains("change") || type.contains("math_step"))
                && event.expressionBefore != null
                && event.expressionAfter != null
                && !event.expressionBefore.equals(event.expressionAfter)
                && !event.expressionBefore.isEmpty()) {
            return true;
        }

        return false;
    }

    /**
     * Answers a direct question from the student.
     * Uses AI to provide contextual help based on the current problem state.
     *
     * Note: Not @Transactional because it calls long-running AI services.
     * DB operations are handled in separate transactional methods
     * (logQuestionInteraction).
     *
     * @param question          The student's question
     * @param currentExpression The current state of the problem (optional)
     * @param initialExpression The original problem state (optional)
     * @param targetExpression  The target solution state (optional)
     * @param sessionId         The session ID (optional)
     * @param context           Conversation context with recent actions, questions,
     *                          and AI messages
     * @param userIdStr         the user ID (as string) for rate limiting
     * @return AI-generated answer
     */
    ChatMessageDto answerQuestion(final String question, final String currentExpression,
            final String initialExpression, final String targetExpression,
            final String sessionId, final ConversationContextDto context, final String userIdStr) {
        if (question == null) {
            throw new IllegalArgumentException("question cannot be null");
        }
        LOG.debugf("Answering question (session: %s, questionLen: %s, contextActions: %s)", 
                sessionId, 
                question.length(), 
                context != null ? context.getRecentActions().size() : 0);

        // Load dynamic configuration (null-safe)
        final Boolean aiEnabled = this.getConfigBoolean(AiConfigKeys.AI_TUTOR_ENABLED, true);
        if (!aiEnabled) {
            return ChatMessageDto.aiAnswer(
                    "I'm currently offline, but keep working on the problem! You can ask your teacher for help.");
        }

        // Use different AI provider based on configuration
        final var aiProvider = this.getConfigString(AiConfigKeys.AI_TUTOR_PROVIDER, "mock");
        final var provider = (aiProvider != null) ? aiProvider.toLowerCase() : "mock";

        // Apply per-user rate limiting for non-mock providers
        final var effectiveUserId = userIdStr != null ? userIdStr : "ANONYMOUS";
        if (!"mock".equals(provider) && !this.checkAiRateLimit(effectiveUserId)) {
            return ChatMessageDto.aiAnswer(
                    "I'm receiving too many requests right now. Please wait a moment before asking again.");
        }

        var answer = switch (provider) {
            case "gemini" ->
                this.safeAnswer(this.geminiAiProvider, question, currentExpression, initialExpression, targetExpression,
                        context);
            case "openai" ->
                this.safeAnswer(this.openAiProvider, question, currentExpression, initialExpression, targetExpression,
                        context);
            case "ollama" ->
                this.safeAnswer(this.ollamaAiProvider, question, currentExpression, initialExpression, targetExpression,
                        context);
            default ->
                this.mockAiProvider.answerQuestion(question, currentExpression, initialExpression, targetExpression,
                        context);
        };

        // Strip leading and trailing quotation marks from AI response
        answer = this.jsonRepairService.stripQuotationMarks(answer);

        final var message = ChatMessageDto.aiAnswer(answer);
        message.sessionId = sessionId;
        return message;
    }

    /**
     * Async version of answerQuestion that returns a CompletableFuture.
     * This allows the UI to show a typing indicator while waiting for the response.
     * Uses Quarkus ManagedExecutor to ensure proper CDI context propagation.
     */
    public CompletableFuture<ChatMessageDto> answerQuestionAsync(final String question,
            final String currentExpression, final String initialExpression, final String targetExpression,
            final String sessionId, final ConversationContextDto context, final String userIdStr) {
        return CompletableFuture.supplyAsync(
                () -> this.answerQuestion(question, currentExpression, initialExpression, targetExpression, sessionId,
                        context, userIdStr),
                this.managedExecutor);
    }

    /**
     * Async version of analyzeMathAction that returns a CompletableFuture.
     * This allows the UI to show a typing indicator while waiting for the response.
     * Uses Quarkus ManagedExecutor to ensure proper CDI context propagation.
     */
    public CompletableFuture<AiFeedbackDto> analyzeMathActionAsync(final GraspableEventDto event,
            final ConversationContextDto context, final String userIdStr) {
        return CompletableFuture.supplyAsync(() -> this.analyzeMathAction(event, context, userIdStr),
                this.managedExecutor);
    }

    /**
     * Generates a new math problem based on student performance.
     *
     * @param difficulty The difficulty level
     * @param category   The problem category (type of math problem)
     * @return A new Graspable Math problem
     */
    public GraspableProblemDto generateProblem(final DifficultyLevel difficulty,
            final GraspableProblemDto.ProblemCategory category) {
        return this.problemGeneratorService.generateProblem(difficulty, category);
    }

    /**
     * Logs an AI interaction to the database for analytics.
     *
     * @param event    The student's action
     * @param feedback The AI's feedback
     */
    public void logInteraction(final GraspableEventDto event, final AiFeedbackDto feedback) {
        this.aiInteractionLogger.logInteraction(event, feedback);
    }

    /**
     * Log a student question and AI answer as an interaction.
     * Used for recording conversational interactions in the SessionDetailsView.
     */
    public void logQuestionInteraction(final String sessionId, final Long userId, final Long exerciseId,
            final String studentQuestion, final String aiAnswer) {
        this.aiInteractionLogger.logQuestionInteraction(sessionId, userId, exerciseId, studentQuestion, aiAnswer);
    }

    // Provider call helpers with fallback to mock

    private AiFeedbackDto safeAnalyze(final AiProvider provider, final GraspableEventDto event,
            final ConversationContextDto context) {
        if (!provider.isAvailable()) {
            LOG.warnf("%s not configured, falling back to mock AI",  provider.getClass().getSimpleName());
            return this.mockAiProvider.analyzeMathAction(event, context);
        }
        try {
            return provider.analyzeMathAction(event, context);
        } catch (final RuntimeException e) {
            LOG.errorf(e, "Error using %s, falling back to mock",  provider.getClass().getSimpleName());
            return this.mockAiProvider.analyzeMathAction(event, context);
        }
    }

    private String safeAnswer(final AiProvider provider, final String question, final String currentExpression,
            final String initialExpression, final String targetExpression,
            final ConversationContextDto context) {
        if (!provider.isAvailable()) {
            LOG.warnf("%s not configured, falling back to mock AI",  provider.getClass().getSimpleName());
            return this.mockAiProvider.answerQuestion(question, currentExpression, initialExpression, targetExpression,
                    context);
        }
        try {
            return provider.answerQuestion(question, currentExpression, initialExpression, targetExpression, context);
        } catch (final RuntimeException e) {
            LOG.errorf(e, "Error using %s, falling back to mock",  provider.getClass().getSimpleName());
            return this.mockAiProvider.answerQuestion(question, currentExpression, initialExpression, targetExpression,
                    context);
        }
    }

    private String getConfigString(final String key, final String defaultValue) {
        if (this.aiConfigService == null) {
            LOG.debugf("AiConfigService not injected, using default for key=%s",  key);
            return defaultValue;
        }
        return this.aiConfigService.getConfigValue(key, defaultValue);
    }

    private Boolean getConfigBoolean(final String key, final Boolean defaultValue) {
        if (this.aiConfigService == null) {
            LOG.debugf("AiConfigService not injected, using default for key=%s",  key);
            return defaultValue;
        }
        return this.aiConfigService.getConfigValueAsBoolean(key, defaultValue);
    }
}
