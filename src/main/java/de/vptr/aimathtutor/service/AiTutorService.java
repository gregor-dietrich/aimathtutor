package de.vptr.aimathtutor.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.vptr.aimathtutor.dto.*;
import de.vptr.aimathtutor.entity.AiInteractionEntity;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.repository.AiInteractionRepository;
import de.vptr.aimathtutor.repository.ExerciseRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * AI Tutor Service - Analyzes student math actions and provides feedback.
 * 
 * Configuration is loaded dynamically from AiConfigService, including:
 * - Whether AI is enabled
 * - Which AI provider to use (mock, gemini, openai, ollama)
 * - Prompt templates for question answering and math tutoring
 * 
 * Can be extended to use OpenAI, Ollama, or other AI services.
 */
@ApplicationScoped
public class AiTutorService {

    private static final Logger LOG = LoggerFactory.getLogger(AiTutorService.class);

    // Smart quote characters for stripping from AI responses
    // Using constants avoids checkstyle's AvoidEscapedUnicodeCharacters warning
    private static final String LEFT_DOUBLE_QUOTE = "\u201C"; // "
    private static final String RIGHT_DOUBLE_QUOTE = "\u201D"; // "
    private static final String LEFT_SINGLE_QUOTE = "\u2018"; // '
    private static final String RIGHT_SINGLE_QUOTE = "\u2019"; // '

    @Inject
    AiConfigService aiConfigService;

    @Inject
    GeminiService geminiService;

    @Inject
    OpenAiService openAiService;

    @Inject
    OllamaService ollamaService;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    UserRepository userRepository;

    @Inject
    ExerciseRepository exerciseRepository;

    @Inject
    AiInteractionRepository aiInteractionRepository;

    @Inject
    ManagedExecutor managedExecutor;

    /**
     * Analyzes a student's math action and provides AI feedback.
     * Only provides feedback for significant actions to reduce spam.
     * 
     * Note: Not @Transactional because it calls long-running AI services.
     * DB operations are handled in separate transactional methods (logInteraction).
     * 
     * @param event   The Graspable Math event containing the student's action
     * @param context Conversation context with recent actions, questions, and AI
     *                messages
     * @return AI-generated feedback, or null if no feedback needed
     */
    public AiFeedbackDto analyzeMathAction(final GraspableEventDto event, final ConversationContextDto context) {
        LOG.info("Analyzing math action: eventType='{}', before='{}', after='{}', context={}",
                event.eventType, event.expressionBefore, event.expressionAfter, context);

        // Load dynamic configuration (null-safe)
        final Boolean aiEnabled = this.getConfigBoolean("ai.tutor.enabled", true);
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
            LOG.info("Skipping feedback for insignificant action: eventType='{}', before='{}', after='{}'",
                    event.eventType, event.expressionBefore, event.expressionAfter);
            return null;
        }

        // Load dynamic provider configuration
        final var aiProvider = this.getConfigString("ai.tutor.provider", "mock");
        LOG.info("Action is significant, generating feedback with provider: {}", aiProvider);

        // Use different AI provider based on configuration
        final var provider = (aiProvider != null) ? aiProvider.toLowerCase() : "mock";
        return switch (provider) {
            case "gemini" -> this.analyzeWithGemini(event, context);
            case "openai" -> this.analyzeWithOpenAi(event, context);
            case "ollama" -> this.analyzeWithOllama(event, context);
            default -> this.analyzeWithMockAi(event);
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
            LOG.debug("Skipping automatic simplification: {}", type);
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
     * @param sessionId         The session ID (optional)
     * @param context           Conversation context with recent actions, questions,
     *                          and AI messages
     * @return AI-generated answer
     */
    public ChatMessageDto answerQuestion(final String question, final String currentExpression,
            final String sessionId, final ConversationContextDto context) {
        LOG.debug("Answering question: {} (session: {}, context: {})", question, sessionId, context);

        // Load dynamic configuration (null-safe)
        final Boolean aiEnabled = this.getConfigBoolean("ai.tutor.enabled", true);
        if (!aiEnabled) {
            return ChatMessageDto.aiAnswer(
                    "I'm currently offline, but keep working on the problem! You can ask your teacher for help.");
        }

        // Use different AI provider based on configuration
        final var aiProvider = this.getConfigString("ai.tutor.provider", "mock");
        final var provider = (aiProvider != null) ? aiProvider.toLowerCase() : "mock";

        var answer = switch (provider) {
            case "gemini" -> this.answerWithGemini(question, currentExpression, context);
            case "openai" -> this.answerWithOpenAi(question, currentExpression, context);
            case "ollama" -> this.answerWithOllama(question, currentExpression, context);
            default -> this.answerWithMockAi(question, currentExpression);
        };

        // Strip leading and trailing quotation marks from AI response
        answer = this.stripQuotationMarks(answer);

        final var message = ChatMessageDto.aiAnswer(answer);
        message.sessionId = sessionId;
        return message;
    }

    /**
     * Async version of answerQuestion that returns a CompletableFuture.
     * This allows the UI to show a typing indicator while waiting for the response.
     * Uses Quarkus ManagedExecutor to ensure proper CDI context propagation.
     *
     * @param question          The student's question
     * @param currentExpression The current math expression
     * @param sessionId         The session identifier
     * @param context           Conversation context
     * @return CompletableFuture containing the AI's answer
     */
    public CompletableFuture<ChatMessageDto> answerQuestionAsync(final String question,
            final String currentExpression, final String sessionId, final ConversationContextDto context) {
        return CompletableFuture.supplyAsync(() -> this.answerQuestion(question, currentExpression, sessionId, context),
                this.managedExecutor);
    }

    /**
     * Async version of analyzeMathAction that returns a CompletableFuture.
     * This allows the UI to show a typing indicator while waiting for the response.
     * Uses Quarkus ManagedExecutor to ensure proper CDI context propagation.
     *
     * @param event   The Graspable Math event
     * @param context Conversation context
     * @return CompletableFuture containing the AI feedback, or null if no feedback
     *         needed
     */
    public CompletableFuture<AiFeedbackDto> analyzeMathActionAsync(final GraspableEventDto event,
            final ConversationContextDto context) {
        return CompletableFuture.supplyAsync(() -> this.analyzeMathAction(event, context),
                this.managedExecutor);
    }

    /**
     * Mock AI implementation using rule-based logic.
     * Provides reasonable feedback based on action type.
     */
    private AiFeedbackDto analyzeWithMockAi(final GraspableEventDto event) {
        final AiFeedbackDto feedback;

        // Analyze based on event type
        switch (event.eventType != null ? event.eventType.toLowerCase() : "") {
            case "simplify":
                if (event.correct != null && event.correct) {
                    feedback = AiFeedbackDto.positive("Great job! You simplified correctly.");
                } else if (event.correct != null && !event.correct) {
                    feedback = AiFeedbackDto.corrective("That simplification doesn't look quite right.");
                    feedback.hints.add("Check if you applied the operation to both sides");
                } else {
                    // No correctness info provided - be less spammy
                    return null; // Don't give feedback for unclear simplification
                }
                break;

            case "expand":
                feedback = AiFeedbackDto.positive("Good expansion!");
                feedback.suggestedNextSteps.add("Now try to simplify the result");
                break;

            case "factor":
                feedback = AiFeedbackDto.positive("Nice factoring!");
                break;

            case "combine":
                feedback = AiFeedbackDto.suggestion("Combining like terms - make sure they match!");
                break;

            // Graspable Math specific action names
            case "addsubinvertaction":
                feedback = AiFeedbackDto.hint("Good! You added/subtracted to both sides to maintain balance.");
                feedback.suggestedNextSteps.add("Continue simplifying to isolate the variable");
                break;

            case "muldivinvertaction":
                feedback = AiFeedbackDto.hint("Nice! You multiplied/divided both sides to maintain balance.");
                feedback.suggestedNextSteps.add("Simplify the result");
                break;

            case "fractioncanceltermsaction":
                feedback = AiFeedbackDto.positive("Great job simplifying the fraction!");
                break;

            case "move":
                // Only give feedback if the expression actually changed
                if (event.expressionBefore != null && event.expressionAfter != null
                        && event.expressionBefore.equals(event.expressionAfter)) {
                    return null; // No change, no feedback
                }
                feedback = AiFeedbackDto.hint("Remember to change the sign when moving across the equals sign!");
                break;

            default:
                // Don't give feedback for unknown or minor actions
                return null;
        }

        feedback.sessionId = event.sessionId;
        feedback.confidence = 0.85; // Mock confidence

        return feedback;
    }

    /**
     * Mock AI implementation for answering student questions.
     */
    private String answerWithMockAi(final String question, final String currentExpression) {
        final var lowerQuestion = question.toLowerCase();

        // Provide context-aware answers based on keywords
        if (lowerQuestion.contains("how") && lowerQuestion.contains("solve")) {
            return "To solve an equation, try to isolate the variable on one side. Work step by step, performing the same operation on both sides.";
        }

        if (lowerQuestion.contains("what") && (lowerQuestion.contains("next") || lowerQuestion.contains("do"))) {
            if (currentExpression != null && currentExpression.contains("+")) {
                return "Try combining like terms or moving terms to isolate the variable.";
            }
            return "Look at what you have now and think about what operation would help simplify or isolate the variable.";
        }

        if (lowerQuestion.contains("why") || lowerQuestion.contains("explain")) {
            return "That's a great question! In algebra, we maintain balance by doing the same operation on both sides. This keeps the equation true.";
        }

        if (lowerQuestion.contains("stuck") || lowerQuestion.contains("help")) {
            return "No worries! Try breaking it down into smaller steps. What's the first thing you can simplify or isolate?";
        }

        if (lowerQuestion.contains("hint")) {
            return "💡 Look for terms you can combine, or operations you can undo to isolate the variable.";
        }

        return "I'm here to help! Can you be more specific about what you're stuck on?";
    }

    /**
     * Answer question using Gemini with conversation context.
     */
    private String answerWithGemini(final String question, final String currentExpression,
            final ConversationContextDto context) {
        if (!this.geminiService.isConfigured()) {
            LOG.warn("Gemini not configured, using mock AI");
            return this.answerWithMockAi(question, currentExpression);
        }

        try {
            final var prompt = this.buildQuestionAnsweringPrompt(question, currentExpression, context);
            return this.geminiService.generateContent(prompt);
        } catch (final Exception e) {
            LOG.error("Error using Gemini for question answering", e);
            return this.answerWithMockAi(question, currentExpression);
        }
    }

    /**
     * Answer question using OpenAI with conversation context.
     */
    private String answerWithOpenAi(final String question, final String currentExpression,
            final ConversationContextDto context) {
        if (!this.openAiService.isConfigured()) {
            LOG.warn("OpenAI not configured, using mock AI");
            return this.answerWithMockAi(question, currentExpression);
        }

        try {
            final var prompt = this.buildQuestionAnsweringPrompt(question, currentExpression, context);
            return this.openAiService.generateContent(prompt);
        } catch (final Exception e) {
            LOG.error("Error using OpenAI for question answering", e);
            return this.answerWithMockAi(question, currentExpression);
        }
    }

    /**
     * Answer question using Ollama with conversation context.
     * Uses MicroProfile Fault Tolerance @Retry for automatic retries with jitter.
     */
    private String answerWithOllama(final String question, final String currentExpression,
            final ConversationContextDto context) {
        if (!this.ollamaService.isAvailable()) {
            LOG.warn("Ollama not available, using mock AI");
            return this.answerWithMockAi(question, currentExpression);
        }

        try {
            return this.callOllamaForQuestion(question, currentExpression, context);
        } catch (final Exception e) {
            LOG.error("Error using Ollama for question answering after retries, falling back to mock", e);
            return this.answerWithMockAi(question, currentExpression);
        }
    }

    /**
     * Internal method to call Ollama for question answering.
     * Separated to allow @Retry annotation to work properly.
     * <p>
     * NOTE: This method must remain package-private (no access modifier) so that
     * CDI proxies
     * can intercept it and apply the @Retry annotation. Making it private would
     * prevent
     * MicroProfile Fault Tolerance from working correctly.
     */
    @Retry(maxRetries = 3, delay = 1000, jitter = 200)
    String callOllamaForQuestion(final String question, final String currentExpression,
            final ConversationContextDto context) {
        final var prompt = this.buildQuestionAnsweringPrompt(question, currentExpression, context);
        return this.ollamaService.generateContent(prompt);
    }

    /**
     * Builds a prompt for answering student questions.
     */
    private String buildQuestionAnsweringPrompt(final String question, final String currentExpression,
            final ConversationContextDto context) {
        final var prompt = new StringBuilder();

        // Load dynamic prompt configuration
        final var prefix = this.getConfigString("ai.prompt.question.answering.prefix",
                "You are a helpful AI math tutor. A student is working on an algebra problem and has asked you a question.");
        final var postfix = this.getConfigString("ai.prompt.question.answering.postfix",
                "Provide a helpful, encouraging answer that:\n- Guides the student's thinking without solving it for them\n- Is concise (2-3 sentences max)\n- Relates to their current problem if possible\n- Uses clear, simple language\n- Encourages them to try the next step\n\nYour answer:");

        prompt.append(prefix);
        prompt.append("\n\n");

        // Add conversation context if available
        if (context != null) {
            if (!context.recentActions.isEmpty()) {
                prompt.append("Recent student actions:\n");
                for (int i = 0; i < context.recentActions.size(); i++) {
                    final var action = context.recentActions.get(i);
                    prompt.append(String.format("%d. %s: '%s' → '%s'%n",
                            i + 1, action.eventType, action.expressionBefore, action.expressionAfter));
                }
                prompt.append("\n");
            }

            if (!context.recentQuestions.isEmpty()) {
                prompt.append("Recent student questions:\n");
                for (int i = 0; i < context.recentQuestions.size(); i++) {
                    final var q = context.recentQuestions.get(i);
                    prompt.append(String.format("%d. \"%s\"%n", i + 1, q.message));
                }
                prompt.append("\n");
            }

            if (!context.recentAiMessages.isEmpty()) {
                prompt.append("Your recent responses:\n");
                for (int i = 0; i < context.recentAiMessages.size(); i++) {
                    final var msg = context.recentAiMessages.get(i);
                    prompt.append(String.format("%d. \"%s\"%n", i + 1, msg.message));
                }
                prompt.append("\n");
            }
        }

        if (currentExpression != null && !currentExpression.isBlank()) {
            prompt.append("Current problem state: ").append(currentExpression).append("\n\n");
        }

        prompt.append("Student question: ").append(question);

        prompt.append("\n\n");
        prompt.append(postfix);

        final var promptString = prompt.toString();
        LOG.debug("Sending QuestionAnsweringPrompt: {}", promptString);

        return promptString;
    }

    /**
     * Analyzes math action using Google Gemini AI with conversation context.
     * Sends a structured prompt and parses JSON response.
     */
    private AiFeedbackDto analyzeWithGemini(final GraspableEventDto event, final ConversationContextDto context) {
        LOG.info("Analyzing math action with Gemini AI");

        // Check if Gemini is configured
        if (!this.geminiService.isConfigured()) {
            LOG.warn("Gemini not configured, falling back to mock AI");
            return this.analyzeWithMockAi(event);
        }

        try {
            // Build the prompt with context
            final var prompt = this.buildMathTutoringPrompt(event, context);

            // Call Gemini API
            final var response = this.geminiService.generateContent(prompt);

            // Parse response as JSON
            return this.parseFeedbackFromJson(response);

        } catch (final Exception e) {
            LOG.error("Error using Gemini AI, falling back to mock", e);
            return this.analyzeWithMockAi(event);
        }
    }

    /**
     * Null-safe getter for string configuration values. Falls back to default if
     * aiConfigService is not injected or value is missing.
     */
    private String getConfigString(final String key, final String defaultValue) {
        if (this.aiConfigService == null) {
            LOG.debug("AiConfigService not injected, using default for key={}", key);
            return defaultValue;
        }
        return this.aiConfigService.getConfigValue(key, defaultValue);
    }

    /**
     * Null-safe getter for boolean configuration values. Falls back to default if
     * aiConfigService is not injected or value is missing.
     */
    private Boolean getConfigBoolean(final String key, final Boolean defaultValue) {
        if (this.aiConfigService == null) {
            LOG.debug("AiConfigService not injected, using default for key={}", key);
            return defaultValue;
        }
        return this.aiConfigService.getConfigValueAsBoolean(key, defaultValue);
    }

    /**
     * Builds a structured prompt for math tutoring with conversation context.
     */
    private String buildMathTutoringPrompt(final GraspableEventDto event, final ConversationContextDto context) {
        final var prompt = new StringBuilder();

        // Load dynamic prompt configuration
        final var prefix = this.getConfigString("ai.prompt.math.tutoring.prefix",
                "You are an encouraging but concise AI math tutor helping a student learn algebra. Analyze the student's action and provide brief, helpful feedback.");
        final var postfix = this.getConfigString("ai.prompt.math.tutoring.postfix",
                "Provide feedback in the following JSON format:\n{\n  \"type\": \"POSITIVE\" or \"CORRECTIVE\" or \"HINT\" or \"SUGGESTION\",\n  \"message\": \"Your brief, encouraging feedback (ONE sentence only)\",\n  \"hints\": [],\n  \"suggestedNextSteps\": [],\n  \"confidence\": 0.0 to 1.0\n}\n\nIMPORTANT Guidelines:\n- Keep message to ONE SHORT sentence (max 15 words)\n- Be encouraging but not overly enthusiastic\n- If the action is correct, give brief praise\n- If incorrect, point out the error gently\n- Only provide hints array if student made a mistake (max 1-2 hints)\n- Do NOT provide hints for correct actions\n- Leave suggestedNextSteps empty unless specifically needed\n- Be specific about what they did, not generic\n");

        prompt.append(prefix);
        prompt.append("\n\nStudent Action:\n- Action Type: ");
        prompt.append(event.eventType != null ? event.eventType : "unknown").append("\n");

        // Add conversation context if available
        if (context != null) {
            if (!context.recentActions.isEmpty()) {
                prompt.append("\nRecent actions (for context):\n");
                for (int i = 0; i < context.recentActions.size(); i++) {
                    final var action = context.recentActions.get(i);
                    prompt.append(String.format("%d. %s: '%s' → '%s'%n",
                            i + 1, action.eventType, action.expressionBefore, action.expressionAfter));
                }
            }

            if (!context.recentQuestions.isEmpty()) {
                prompt.append("\nRecent student questions:\n");
                for (int i = 0; i < context.recentQuestions.size(); i++) {
                    final var q = context.recentQuestions.get(i);
                    prompt.append(String.format("%d. \"%s\"%n", i + 1, q.message));
                }
            }

            if (!context.recentAiMessages.isEmpty()) {
                prompt.append("\nYour recent feedback:\n");
                for (int i = 0; i < context.recentAiMessages.size(); i++) {
                    final var msg = context.recentAiMessages.get(i);
                    prompt.append(String.format("%d. \"%s\"%n", i + 1, msg.message));
                }
            }
        }

        prompt.append("\nCurrent action being analyzed:\n");
        if (event.expressionBefore != null) {
            prompt.append("- Expression Before: ").append(event.expressionBefore).append("\n");
        }

        if (event.expressionAfter != null) {
            prompt.append("- Expression After: ").append(event.expressionAfter).append("\n");
        }

        if (event.correct != null) {
            prompt.append("- Is Correct: ").append(event.correct).append("\n");
        }

        prompt.append('\n');
        prompt.append(postfix);

        final var promptString = prompt.toString();
        LOG.debug("Sending MathTutoringPrompt: {}", promptString);

        return promptString;
    }

    /**
     * Parses AI provider's JSON response into AIFeedbackDto.
     * Falls back to extracting message if JSON parsing fails.
     */
    private AiFeedbackDto parseFeedbackFromJson(final String jsonResponse) {
        try {
            // Try to extract JSON from response (AI provider might wrap it in markdown)
            var json = jsonResponse.trim();

            // Remove markdown code block if present
            if (json.startsWith("```json")) {
                json = json.substring(7);
            }
            if (json.startsWith("```")) {
                json = json.substring(3);
            }
            if (json.endsWith("```")) {
                json = json.substring(0, json.length() - 3);
            }
            json = json.trim();

            // Parse JSON to AIFeedbackDto
            final var feedback = this.objectMapper.readValue(json, AiFeedbackDto.class);

            // Set timestamp
            feedback.timestamp = LocalDateTime.now();

            LOG.debug("Successfully parsed AI provider response as JSON");
            return feedback;

        } catch (final Exception e) {
            LOG.warn("Failed to parse AI provider response as JSON, creating simple feedback", e);

            // Fallback: create simple positive feedback with the response text
            final var feedback = AiFeedbackDto.positive(jsonResponse);
            feedback.confidence = 0.7;
            return feedback;
        }
    }

    /**
     * Analyzes math action using OpenAI (GPT models) with conversation context.
     * Uses JSON mode for guaranteed valid JSON responses.
     */
    private AiFeedbackDto analyzeWithOpenAi(final GraspableEventDto event, final ConversationContextDto context) {
        LOG.info("Analyzing math action with OpenAI");

        // Check if OpenAI is configured
        if (!this.openAiService.isConfigured()) {
            LOG.warn("OpenAI not configured, falling back to mock AI");
            return this.analyzeWithMockAi(event);
        }

        try {
            // Build the prompt with context
            final var prompt = this.buildMathTutoringPrompt(event, context);

            // Call OpenAI API with JSON mode
            final var response = this.openAiService.generateJsonContent(prompt);

            // Parse response as JSON
            return this.parseFeedbackFromJson(response);

        } catch (final Exception e) {
            LOG.error("Error using OpenAI, falling back to mock", e);
            return this.analyzeWithMockAi(event);
        }
    }

    /**
     * Analyzes math action using Ollama (local LLM) with conversation context.
     * Sends structured prompt and parses JSON response.
     * Uses MicroProfile Fault Tolerance @Retry for automatic retries with jitter.
     */
    private AiFeedbackDto analyzeWithOllama(final GraspableEventDto event, final ConversationContextDto context) {
        LOG.info("Analyzing math action with Ollama");

        // Check if Ollama is available
        if (!this.ollamaService.isAvailable()) {
            LOG.warn("Ollama server not available at {}, falling back to mock AI", this.ollamaService.getApiUrl());
            return this.analyzeWithMockAi(event);
        }

        try {
            return this.callOllamaForAnalysis(event, context);
        } catch (final Exception e) {
            LOG.error("Error using Ollama after retries, falling back to mock", e);
            return this.analyzeWithMockAi(event);
        }
    }

    /**
     * Internal method to call Ollama for math action analysis.
     * Separated to allow @Retry annotation to work properly.
     * <p>
     * NOTE: This method must remain package-private (no access modifier) so that
     * CDI proxies
     * can intercept it and apply the @Retry annotation. Making it private would
     * prevent
     * MicroProfile Fault Tolerance from working correctly.
     */
    @Retry(maxRetries = 3, delay = 1000, jitter = 200)
    AiFeedbackDto callOllamaForAnalysis(final GraspableEventDto event, final ConversationContextDto context) {
        // Build the prompt with context
        final var prompt = this.buildMathTutoringPrompt(event, context);

        // Call Ollama API
        final var response = this.ollamaService.generateContent(prompt);

        // Parse response as JSON (always returns non-null, with fallback on parse
        // error)
        return this.parseFeedbackFromJson(response);
    }

    /**
     * Generates a new math problem based on student performance.
     * 
     * @param difficulty The difficulty level
     * @param category   The problem category (type of math problem)
     * @return A new Graspable Math problem
     */
    public GraspableProblemDto generateProblem(final String difficulty,
            final GraspableProblemDto.ProblemCategory category) {
        LOG.debug("Generating problem: difficulty={}, category={}", difficulty, category);

        final var problem = new GraspableProblemDto();
        problem.difficulty = difficulty;
        problem.category = category != null ? category : GraspableProblemDto.ProblemCategory.LINEAR_EQUATIONS;

        // Generate random problems based on category
        // ThreadLocalRandom is used (not SecureRandom) because:
        // - Problem generation does not require cryptographic security
        // - ThreadLocalRandom is more efficient for concurrent access
        // - Each thread has its own random instance, avoiding contention
        // - No initialization overhead compared to SecureRandom
        final var random = ThreadLocalRandom.current();

        switch (problem.category) {
            case LINEAR_EQUATIONS -> {
                // Generate random linear equation: ax + b = c
                final int a = random.nextInt(9) + 1; // 1-9
                final int b = random.nextInt(20) - 10; // -10 to 10
                final int x = random.nextInt(20) - 10; // -10 to 10
                final int c = a * x + b;
                problem.title = "Solve for x";
                problem.initialExpression = String.format("%dx %s %d = %d",
                        a,
                        b >= 0 ? "+" : "-",
                        Math.abs(b),
                        c);
                problem.targetExpression = "x = " + x;
                problem.allowedOperations.addAll(Arrays.asList("simplify", "move", "divide"));
                problem.hints.add("First, isolate the term with x");
                problem.hints.add("Remember to do the same operation on both sides");
            }
            case QUADRATIC_EQUATIONS -> {
                // Generate simple quadratic: x^2 = n (perfect square)
                final int sqrtVal = random.nextInt(10) + 1; // 1-10
                final int nSquared = sqrtVal * sqrtVal;
                problem.title = "Solve for x";
                problem.initialExpression = String.format("x^2 = %d", nSquared);
                problem.targetExpression = String.format("x = ±%d", sqrtVal);
                problem.allowedOperations.addAll(Arrays.asList("sqrt", "simplify"));
                problem.hints.add("Take the square root of both sides");
                problem.hints.add("Remember there are two solutions: positive and negative");
            }
            case POLYNOMIAL_SIMPLIFICATION -> {
                // Generate random simplification
                final int coef1 = random.nextInt(9) + 1;
                final int coef2 = random.nextInt(9) + 1;
                problem.title = "Simplify the expression";
                problem.initialExpression = String.format("%dx + %dx", coef1, coef2);
                problem.targetExpression = (coef1 + coef2) + "x";
                problem.allowedOperations.addAll(Arrays.asList("simplify", "combine"));
                problem.hints.add("Combine like terms");
                problem.hints.add("Add the coefficients of x");
            }
            case FACTORING -> {
                // Generate random factorable quadratic
                final int p = random.nextInt(9) + 1;
                final int q = random.nextInt(9) + 1;
                final int sum = p + q;
                final int product = p * q;
                problem.title = "Factor the expression";
                problem.initialExpression = String.format("x^2 + %dx + %d", sum, product);
                problem.targetExpression = String.format("(x + %d)(x + %d)", p, q);
                problem.allowedOperations.addAll(Arrays.asList("factor", "expand"));
                problem.hints
                        .add(String.format("Look for two numbers that multiply to %d and add to %d", product, sum));
            }
            case FRACTIONS -> {
                // Generate fraction addition: a/b + c/d
                final int num1 = random.nextInt(5) + 1;
                final int den1 = random.nextInt(5) + 2;
                final int num2 = random.nextInt(5) + 1;
                final int den2 = random.nextInt(5) + 2;
                problem.title = "Add the fractions";
                problem.initialExpression = String.format("%d/%d + %d/%d", num1, den1, num2, den2);
                problem.targetExpression = "Simplified form";
                problem.allowedOperations.addAll(Arrays.asList("simplify", "add"));
                problem.hints.add("Find a common denominator");
                problem.hints.add("Add the numerators");
            }
            case EXPONENTS -> {
                // Generate exponent simplification: x^a * x^b = x^(a+b)
                final int exp1 = random.nextInt(4) + 2; // 2-5
                final int exp2 = random.nextInt(4) + 2; // 2-5
                problem.title = "Simplify using exponent rules";
                problem.initialExpression = String.format("x^%d * x^%d", exp1, exp2);
                problem.targetExpression = String.format("x^%d", exp1 + exp2);
                problem.allowedOperations.addAll(Arrays.asList("simplify", "multiply"));
                problem.hints.add("When multiplying powers with the same base, add the exponents");
                problem.hints.add(String.format("x^%d * x^%d = x^(%d+%d)", exp1, exp2, exp1, exp2));
            }
            case SYSTEMS_OF_EQUATIONS -> {
                // Generate simple system (substitution method)
                final int yVal = random.nextInt(10) + 1;
                final int xVal = random.nextInt(10) + 1;
                final int coefX = random.nextInt(3) + 1;
                problem.title = "Solve the system of equations";
                problem.initialExpression = String.format("y = %d; %dx + y = %d", yVal, coefX, coefX * xVal + yVal);
                problem.targetExpression = String.format("x = %d; y = %d", xVal, yVal);
                problem.allowedOperations.addAll(Arrays.asList("substitute", "solve", "simplify"));
                problem.hints.add("Substitute the value of y from the first equation into the second");
                problem.hints.add("Solve for x, then verify with y");
            }
            case INEQUALITIES -> {
                // Generate simple inequality: ax + b < c
                final int aIneq = random.nextInt(5) + 1;
                final int bIneq = random.nextInt(10) - 5;
                final int cIneq = random.nextInt(20);
                problem.title = "Solve the inequality";
                problem.initialExpression = String.format("%dx %s %d < %d",
                        aIneq,
                        bIneq >= 0 ? "+" : "-",
                        Math.abs(bIneq),
                        cIneq);
                problem.targetExpression = String.format("x < %d", (cIneq - bIneq) / aIneq);
                problem.allowedOperations.addAll(Arrays.asList("simplify", "move", "divide"));
                problem.hints.add("Solve like an equation, but keep the inequality sign");
                problem.hints.add("Remember: if dividing by a negative number, flip the inequality");
            }
            default -> {
                // Fallback to linear equations
                return this.generateProblem(difficulty, GraspableProblemDto.ProblemCategory.LINEAR_EQUATIONS);
            }
        }

        return problem;
    }

    /**
     * Logs an AI interaction to the database for analytics.
     * 
     * @param event    The student's action
     * @param feedback The AI's feedback
     */
    @Transactional
    public void logInteraction(final GraspableEventDto event, final AiFeedbackDto feedback) {
        try {
            final var interaction = new AiInteractionEntity();
            interaction.sessionId = event.sessionId;

            // Look up user and exercise if IDs are provided
            if (event.studentId != null) {
                interaction.user = this.userRepository.findById(event.studentId);
            }
            if (event.exerciseId != null) {
                interaction.exercise = this.exerciseRepository.findById(event.exerciseId);
            }

            interaction.eventType = event.eventType;
            interaction.expressionBefore = event.expressionBefore;
            interaction.expressionAfter = event.expressionAfter;
            interaction.feedbackType = feedback.type.toString();
            interaction.feedbackMessage = feedback.message;
            interaction.confidenceScore = feedback.confidence;
            interaction.actionCorrect = event.correct;

            this.aiInteractionRepository.persist(interaction);
            LOG.debug("Logged AI interaction: {}", interaction.id);
        } catch (final Exception e) {
            LOG.error("Failed to log AI interaction", e);
            // Don't fail the request if logging fails
        }
    }

    /**
     * Strips matching leading and trailing quotation marks from a string.
     * Only removes quotes if they wrap the entire string (both start and end
     * match).
     * Handles both double quotes (") and smart quotes.
     * <p>
     * NOTE: This method is package-private (no access modifier) rather than private
     * to allow unit testing. See AITutorServiceTest for test coverage.
     * 
     * @param text The text to process
     * @return The text with quotation marks removed, or the original text if null
     */
    String stripQuotationMarks(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        text = text.trim();

        // Remove matching quotation marks (only if both start and end match)
        // Handles: "text", "text" (smart double), 'text' (smart single)
        // Length check (> 1) prevents StringIndexOutOfBoundsException for single-char
        // strings
        while (text.length() > 1) {
            if ((text.startsWith("\"") && text.endsWith("\""))
                    || (text.startsWith(LEFT_DOUBLE_QUOTE) && text.endsWith(RIGHT_DOUBLE_QUOTE))
                    || (text.startsWith(LEFT_SINGLE_QUOTE) && text.endsWith(RIGHT_SINGLE_QUOTE))) {
                text = text.substring(1, text.length() - 1).trim();
            } else {
                break;
            }
        }

        return text;
    }

    /**
     * Log a student question and AI answer as an interaction.
     * Used for recording conversational interactions in the SessionDetailsView.
     * Marked as @Transactional to ensure proper persistence in async contexts.
     */
    @Transactional
    public void logQuestionInteraction(final String sessionId, final Long userId, final Long exerciseId,
            final String studentQuestion, final String aiAnswer) {
        try {
            LOG.info("Logging question interaction: sessionId={}, userId={}, exerciseId={}, question={}, answer={}",
                    sessionId, userId, exerciseId, studentQuestion, aiAnswer);

            // Create TWO separate records: one for student question, one for AI answer
            // This ensures they appear as separate rows in the SessionDetailView grid

            // 1. Log the student question
            final var studentQuestionRecord = new AiInteractionEntity();
            studentQuestionRecord.sessionId = sessionId;
            studentQuestionRecord.eventType = "QUESTION";
            studentQuestionRecord.feedbackType = "QUESTION";
            studentQuestionRecord.studentMessage = studentQuestion;

            UserEntity user = null;
            if (userId != null) {
                user = this.userRepository.findById(userId);
                if (user == null) {
                    LOG.warn("User not found for logging question interaction: userId={}", userId);
                } else {
                    studentQuestionRecord.user = user;
                }
            }

            ExerciseEntity exercise = null;
            if (exerciseId != null) {
                exercise = this.exerciseRepository.findById(exerciseId);
                if (exercise == null) {
                    LOG.warn("Exercise not found for logging question interaction: exerciseId={}", exerciseId);
                } else {
                    studentQuestionRecord.exercise = exercise;
                }
            }

            this.aiInteractionRepository.persist(studentQuestionRecord);
            LOG.info("Successfully logged student question: id={}, studentMessage={}",
                    studentQuestionRecord.id, studentQuestionRecord.studentMessage);

            // 2. Log the AI answer as a separate record
            final var aiAnswerRecord = new AiInteractionEntity();
            aiAnswerRecord.sessionId = sessionId;
            aiAnswerRecord.eventType = "QUESTION_ANSWER";
            aiAnswerRecord.feedbackType = "ANSWER";
            aiAnswerRecord.feedbackMessage = aiAnswer;

            if (userId != null) {
                user = this.userRepository.findById(userId);
                if (user == null) {
                    LOG.warn("User not found for logging AI answer: userId={}", userId);
                } else {
                    aiAnswerRecord.user = user;
                }
            }

            if (exerciseId != null) {
                exercise = this.exerciseRepository.findById(exerciseId);
                if (exercise == null) {
                    LOG.warn("Exercise not found for logging AI answer: exerciseId={}", exerciseId);
                } else {
                    aiAnswerRecord.exercise = exercise;
                }
            }

            this.aiInteractionRepository.persist(aiAnswerRecord);
            LOG.info("Successfully logged AI answer: id={}, feedbackMessage={}",
                    aiAnswerRecord.id, aiAnswerRecord.feedbackMessage);
        } catch (final Exception e) {
            LOG.error("Failed to log question interaction", e);
            // Don't fail the request if logging fails
        }
    }
}
