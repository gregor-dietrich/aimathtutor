package de.vptr.aimathtutor.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.vptr.aimathtutor.dto.*;
import de.vptr.aimathtutor.entity.AIInteractionEntity;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * AI Tutor Service - Analyzes student math actions and provides feedback.
 * 
 * Current implementation uses rule-based logic (mock/placeholder).
 * Can be extended to use OpenAI, Ollama, or other AI services.
 */
@ApplicationScoped
public class AITutorService {

    private static final Logger LOG = LoggerFactory.getLogger(AITutorService.class);

    private static final String questionAnsweringPromptPrefix = "You are a helpful AI math tutor. A student is working on an algebra problem and has asked you a question.\n\n";
    private static final String questionAnsweringPromptPostfix = "\n\nProvide a helpful, encouraging answer that:\n- Guides the student's thinking without solving it for them\n- Is concise (2-3 sentences max)\n- Relates to their current problem if possible\n- Uses clear, simple language\n- Encourages them to try the next step\n\nYour answer:";

    private static final String mathTutoringPromptPrefix = "You are an encouraging but concise AI math tutor helping a student learn algebra. Analyze the student's action and provide brief, helpful feedback.\n\nStudent Action:\n- Action Type: ";
    private static final String mathTutoringPromptPostfix = "\nProvide feedback in the following JSON format:\n{\n  \"type\": \"POSITIVE\" or \"CORRECTIVE\" or \"HINT\" or \"SUGGESTION\",\n  \"message\": \"Your brief, encouraging feedback (ONE sentence only)\",\n  \"hints\": [],\n  \"suggestedNextSteps\": [],\n  \"confidence\": 0.0 to 1.0\n}\n\nIMPORTANT Guidelines:\n- Keep message to ONE SHORT sentence (max 15 words)\n- Be encouraging but not overly enthusiastic\n- If the action is correct, give brief praise\n- If incorrect, point out the error gently\n- Only provide hints array if student made a mistake (max 1-2 hints)\n- Do NOT provide hints for correct actions\n- Leave suggestedNextSteps empty unless specifically needed\n- Be specific about what they did, not generic\n";

    @ConfigProperty(name = "ai.tutor.enabled", defaultValue = "true")
    Boolean aiEnabled;

    @ConfigProperty(name = "ai.tutor.provider", defaultValue = "mock")
    String aiProvider; // "mock", "openai", "ollama", "gemini"

    @Inject
    GeminiAIService geminiService;

    @Inject
    OpenAIService openAIService;

    @Inject
    OllamaService ollamaService;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Analyzes a student's math action and provides AI feedback.
     * Only provides feedback for significant actions to reduce spam.
     * 
     * @param event   The Graspable Math event containing the student's action
     * @param context Conversation context with recent actions, questions, and AI
     *                messages
     * @return AI-generated feedback, or null if no feedback needed
     */
    public AIFeedbackDto analyzeMathAction(final GraspableEventDto event, final ConversationContextDto context) {
        LOG.info("Analyzing math action: eventType='{}', before='{}', after='{}', context={}",
                event.eventType, event.expressionBefore, event.expressionAfter, context);

        if (this.aiEnabled != null && !this.aiEnabled) {
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

        LOG.info("Action is significant, generating feedback with provider: {}", this.aiProvider);

        // Use different AI provider based on configuration
        final String provider = (this.aiProvider != null) ? this.aiProvider.toLowerCase() : "mock";
        return switch (provider) {
            case "gemini" -> this.analyzeWithGemini(event, context);
            case "openai" -> this.analyzeWithOpenAI(event, context);
            case "ollama" -> this.analyzeWithOllama(event, context);
            default -> this.analyzeWithMockAI(event);
        };
    }

    /**
     * Generates congratulatory feedback when a problem is completed.
     * 
     * @param event The completion event
     * @return Congratulatory feedback message
     */
    private AIFeedbackDto generateCompletionFeedback(final GraspableEventDto event) {
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
        final String message = congratulatoryMessages[index];

        return AIFeedbackDto.positive(message);
    }

    /**
     * Determines if an action is significant enough to warrant feedback.
     * Helps reduce spammy feedback on every minor change.
     */
    private boolean isSignificantAction(final GraspableEventDto event) {
        if (event.eventType == null) {
            return false;
        }

        final String type = event.eventType.toLowerCase();

        // Graspable Math specific action names (from the actual GM API)
        if (type.contains("addsubinvert") || // Adding/subtracting to both sides
                type.contains("muldivinvert") || // Multiplying/dividing both sides
                type.contains("fractioncancel") || // Simplifying fractions
                type.contains("distribute") || // Distributing multiplication
                type.contains("factor") || // Factoring
                type.contains("collect") || // Collecting like terms
                type.contains("commute") || // Commutative property
                type.contains("associate")) { // Associative property
            return true;
        }

        // Generic action keywords (might be used by other math tools or custom actions)
        if (type.contains("simplify") ||
                type.contains("expand") ||
                type.contains("solve") ||
                type.contains("combine") ||
                type.contains("isolate") ||
                type.contains("substitute")) {
            return true;
        }

        // Skip automatic simplification actions to reduce noise
        if (type.contains("postinteraction") ||
                type.contains("addsubnumbers") || // Automatic number combining
                type.contains("autosimp")) {
            LOG.debug("Skipping automatic simplification: {}", type);
            return false;
        }

        // For generic "change" or "math_step" events, check if expression actually
        // changed
        if ((type.contains("change") || type.contains("math_step")) &&
                event.expressionBefore != null &&
                event.expressionAfter != null &&
                !event.expressionBefore.equals(event.expressionAfter) &&
                !event.expressionBefore.isEmpty()) {
            return true;
        }

        return false;
    }

    /**
     * Answers a direct question from the student.
     * Uses AI to provide contextual help based on the current problem state.
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

        if (this.aiEnabled != null && !this.aiEnabled) {
            return ChatMessageDto.aiAnswer(
                    "I'm currently offline, but keep working on the problem! You can ask your teacher for help.");
        }

        // Use different AI provider based on configuration
        final String provider = (this.aiProvider != null) ? this.aiProvider.toLowerCase() : "mock";

        final String answer = switch (provider) {
            case "gemini" -> this.answerWithGemini(question, currentExpression, context);
            case "openai" -> this.answerWithOpenAI(question, currentExpression, context);
            case "ollama" -> this.answerWithOllama(question, currentExpression, context);
            default -> this.answerWithMockAI(question, currentExpression);
        };

        final var message = ChatMessageDto.aiAnswer(answer);
        message.sessionId = sessionId;
        return message;
    }

    /**
     * Async version of answerQuestion that returns a CompletableFuture.
     * This allows the UI to show a typing indicator while waiting for the response.
     * 
     * @param question          The student's question
     * @param currentExpression The current math expression
     * @param sessionId         The session identifier
     * @param context           Conversation context
     * @return CompletableFuture containing the AI's answer
     */
    public CompletableFuture<ChatMessageDto> answerQuestionAsync(final String question,
            final String currentExpression, final String sessionId, final ConversationContextDto context) {
        return CompletableFuture
                .supplyAsync(() -> this.answerQuestion(question, currentExpression, sessionId, context));
    }

    /**
     * Async version of analyzeMathAction that returns a CompletableFuture.
     * This allows the UI to show a typing indicator while waiting for the response.
     * 
     * @param event   The Graspable Math event
     * @param context Conversation context
     * @return CompletableFuture containing the AI feedback, or null if no feedback
     *         needed
     */
    public CompletableFuture<AIFeedbackDto> analyzeMathActionAsync(final GraspableEventDto event,
            final ConversationContextDto context) {
        return CompletableFuture.supplyAsync(() -> this.analyzeMathAction(event, context));
    }

    /**
     * Mock AI implementation using rule-based logic.
     * Provides reasonable feedback based on action type.
     */
    private AIFeedbackDto analyzeWithMockAI(final GraspableEventDto event) {
        final AIFeedbackDto feedback;

        // Analyze based on event type
        switch (event.eventType != null ? event.eventType.toLowerCase() : "") {
            case "simplify":
                if (event.correct != null && event.correct) {
                    feedback = AIFeedbackDto.positive("Great job! You simplified correctly.");
                } else if (event.correct != null && !event.correct) {
                    feedback = AIFeedbackDto.corrective("That simplification doesn't look quite right.");
                    feedback.hints.add("Check if you applied the operation to both sides");
                } else {
                    // No correctness info provided - be less spammy
                    return null; // Don't give feedback for unclear simplification
                }
                break;

            case "expand":
                feedback = AIFeedbackDto.positive("Good expansion!");
                feedback.suggestedNextSteps.add("Now try to simplify the result");
                break;

            case "factor":
                feedback = AIFeedbackDto.positive("Nice factoring!");
                break;

            case "combine":
                feedback = AIFeedbackDto.suggestion("Combining like terms - make sure they match!");
                break;

            // Graspable Math specific action names
            case "addsubinvertaction":
                feedback = AIFeedbackDto.hint("Good! You added/subtracted to both sides to maintain balance.");
                feedback.suggestedNextSteps.add("Continue simplifying to isolate the variable");
                break;

            case "muldivinvertaction":
                feedback = AIFeedbackDto.hint("Nice! You multiplied/divided both sides to maintain balance.");
                feedback.suggestedNextSteps.add("Simplify the result");
                break;

            case "fractioncanceltermsaction":
                feedback = AIFeedbackDto.positive("Great job simplifying the fraction!");
                break;

            case "move":
                // Only give feedback if the expression actually changed
                if (event.expressionBefore != null && event.expressionAfter != null &&
                        event.expressionBefore.equals(event.expressionAfter)) {
                    return null; // No change, no feedback
                }
                feedback = AIFeedbackDto.hint("Remember to change the sign when moving across the equals sign!");
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
    private String answerWithMockAI(final String question, final String currentExpression) {
        final String lowerQuestion = question.toLowerCase();

        // Provide context-aware answers based on keywords
        if (lowerQuestion.contains("how") && lowerQuestion.contains("solve")) {
            return "To solve an equation, try to isolate the variable on one side. Work step by step, performing the same operation on both sides.";
        } else if (lowerQuestion.contains("what") && (lowerQuestion.contains("next") || lowerQuestion.contains("do"))) {
            if (currentExpression != null && currentExpression.contains("+")) {
                return "Try combining like terms or moving terms to isolate the variable.";
            }
            return "Look at what you have now and think about what operation would help simplify or isolate the variable.";
        } else if (lowerQuestion.contains("why") || lowerQuestion.contains("explain")) {
            return "That's a great question! In algebra, we maintain balance by doing the same operation on both sides. This keeps the equation true.";
        } else if (lowerQuestion.contains("stuck") || lowerQuestion.contains("help")) {
            return "No worries! Try breaking it down into smaller steps. What's the first thing you can simplify or isolate?";
        } else if (lowerQuestion.contains("hint")) {
            return "💡 Look for terms you can combine, or operations you can undo to isolate the variable.";
        } else {
            return "I'm here to help! Can you be more specific about what you're stuck on?";
        }
    }

    /**
     * Answer question using Gemini with conversation context.
     */
    private String answerWithGemini(final String question, final String currentExpression,
            final ConversationContextDto context) {
        if (!this.geminiService.isConfigured()) {
            LOG.warn("Gemini not configured, using mock AI");
            return this.answerWithMockAI(question, currentExpression);
        }

        try {
            final String prompt = this.buildQuestionAnsweringPrompt(question, currentExpression, context);
            return this.geminiService.generateContent(prompt);
        } catch (final Exception e) {
            LOG.error("Error using Gemini for question answering", e);
            return this.answerWithMockAI(question, currentExpression);
        }
    }

    /**
     * Answer question using OpenAI with conversation context.
     */
    private String answerWithOpenAI(final String question, final String currentExpression,
            final ConversationContextDto context) {
        if (!this.openAIService.isConfigured()) {
            LOG.warn("OpenAI not configured, using mock AI");
            return this.answerWithMockAI(question, currentExpression);
        }

        try {
            final String prompt = this.buildQuestionAnsweringPrompt(question, currentExpression, context);
            return this.openAIService.generateContent(prompt);
        } catch (final Exception e) {
            LOG.error("Error using OpenAI for question answering", e);
            return this.answerWithMockAI(question, currentExpression);
        }
    }

    /**
     * Answer question using Ollama with conversation context.
     */
    private String answerWithOllama(final String question, final String currentExpression,
            final ConversationContextDto context) {
        if (!this.ollamaService.isAvailable()) {
            LOG.warn("Ollama not available, using mock AI");
            return this.answerWithMockAI(question, currentExpression);
        }

        try {
            final var prompt = this.buildQuestionAnsweringPrompt(question, currentExpression, context);
            return this.ollamaService.generateContent(prompt);
        } catch (final Exception e) {
            LOG.error("Error using Ollama for question answering", e);
            return this.answerWithMockAI(question, currentExpression);
        }
    }

    /**
     * Builds a prompt for answering student questions.
     */
    private String buildQuestionAnsweringPrompt(final String question, final String currentExpression,
            final ConversationContextDto context) {
        final var prompt = new StringBuilder();

        prompt.append(questionAnsweringPromptPrefix);

        // Add conversation context if available
        if (context != null) {
            if (!context.recentActions.isEmpty()) {
                prompt.append("Recent student actions:\n");
                for (int i = 0; i < context.recentActions.size(); i++) {
                    final var action = context.recentActions.get(i);
                    prompt.append(String.format("%d. %s: '%s' → '%s'\n",
                            i + 1, action.eventType, action.expressionBefore, action.expressionAfter));
                }
                prompt.append("\n");
            }

            if (!context.recentQuestions.isEmpty()) {
                prompt.append("Recent student questions:\n");
                for (int i = 0; i < context.recentQuestions.size(); i++) {
                    final var q = context.recentQuestions.get(i);
                    prompt.append(String.format("%d. \"%s\"\n", i + 1, q.message));
                }
                prompt.append("\n");
            }

            if (!context.recentAIMessages.isEmpty()) {
                prompt.append("Your recent responses:\n");
                for (int i = 0; i < context.recentAIMessages.size(); i++) {
                    final var msg = context.recentAIMessages.get(i);
                    prompt.append(String.format("%d. \"%s\"\n", i + 1, msg.message));
                }
                prompt.append("\n");
            }
        }

        if (currentExpression != null && !currentExpression.trim().isEmpty()) {
            prompt.append("Current problem state: ").append(currentExpression).append("\n\n");
        }

        prompt.append("Student question: ").append(question);

        prompt.append(questionAnsweringPromptPostfix);

        final var promptString = prompt.toString();
        LOG.debug("Sending QuestionAnsweringPrompt: {}", promptString);

        return promptString;
    }

    /**
     * Analyzes math action using Google Gemini AI with conversation context.
     * Sends a structured prompt and parses JSON response.
     */
    private AIFeedbackDto analyzeWithGemini(final GraspableEventDto event, final ConversationContextDto context) {
        LOG.info("Analyzing math action with Gemini AI");

        // Check if Gemini is configured
        if (!this.geminiService.isConfigured()) {
            LOG.warn("Gemini not configured, falling back to mock AI");
            return this.analyzeWithMockAI(event);
        }

        try {
            // Build the prompt with context
            final var prompt = this.buildMathTutoringPrompt(event, context);

            // Call Gemini API
            final var response = this.geminiService.generateContent(prompt);

            // Parse response as JSON
            return this.parseFeedbackFromJSON(response);

        } catch (final Exception e) {
            LOG.error("Error using Gemini AI, falling back to mock", e);
            return this.analyzeWithMockAI(event);
        }
    }

    /**
     * Builds a structured prompt for math tutoring with conversation context.
     */
    private String buildMathTutoringPrompt(final GraspableEventDto event, final ConversationContextDto context) {
        final var prompt = new StringBuilder();

        prompt.append(mathTutoringPromptPrefix);

        prompt.append(event.eventType != null ? event.eventType : "unknown").append("\n");

        // Add conversation context if available
        if (context != null) {
            if (!context.recentActions.isEmpty()) {
                prompt.append("\nRecent actions (for context):\n");
                for (int i = 0; i < context.recentActions.size(); i++) {
                    final var action = context.recentActions.get(i);
                    prompt.append(String.format("%d. %s: '%s' → '%s'\n",
                            i + 1, action.eventType, action.expressionBefore, action.expressionAfter));
                }
            }

            if (!context.recentQuestions.isEmpty()) {
                prompt.append("\nRecent student questions:\n");
                for (int i = 0; i < context.recentQuestions.size(); i++) {
                    final var q = context.recentQuestions.get(i);
                    prompt.append(String.format("%d. \"%s\"\n", i + 1, q.message));
                }
            }

            if (!context.recentAIMessages.isEmpty()) {
                prompt.append("\nYour recent feedback:\n");
                for (int i = 0; i < context.recentAIMessages.size(); i++) {
                    final var msg = context.recentAIMessages.get(i);
                    prompt.append(String.format("%d. \"%s\"\n", i + 1, msg.message));
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

        prompt.append(mathTutoringPromptPostfix);

        final var promptString = prompt.toString();
        LOG.debug("Sending MathTutoringPrompt: {}", promptString);

        return promptString;
    }

    /**
     * Parses Gemini's JSON response into AIFeedbackDto.
     * Falls back to extracting message if JSON parsing fails.
     */
    private AIFeedbackDto parseFeedbackFromJSON(final String jsonResponse) {
        try {
            // Try to extract JSON from response (Gemini might wrap it in markdown)
            String json = jsonResponse.trim();

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
            final var feedback = this.objectMapper.readValue(json, AIFeedbackDto.class);

            // Set timestamp
            feedback.timestamp = LocalDateTime.now();

            LOG.debug("Successfully parsed Gemini response as JSON");
            return feedback;

        } catch (final Exception e) {
            LOG.warn("Failed to parse Gemini response as JSON, creating simple feedback", e);

            // Fallback: create simple positive feedback with the response text
            final var feedback = AIFeedbackDto.positive(jsonResponse);
            feedback.confidence = 0.7;
            return feedback;
        }
    }

    /**
     * Analyzes math action using OpenAI (GPT models) with conversation context.
     * Uses JSON mode for guaranteed valid JSON responses.
     */
    private AIFeedbackDto analyzeWithOpenAI(final GraspableEventDto event, final ConversationContextDto context) {
        LOG.info("Analyzing math action with OpenAI");

        // Check if OpenAI is configured
        if (!this.openAIService.isConfigured()) {
            LOG.warn("OpenAI not configured, falling back to mock AI");
            return this.analyzeWithMockAI(event);
        }

        try {
            // Build the prompt with context
            final String prompt = this.buildMathTutoringPrompt(event, context);

            // Call OpenAI API with JSON mode
            final String response = this.openAIService.generateJsonContent(prompt);

            // Parse response as JSON
            return this.parseFeedbackFromJSON(response);

        } catch (final Exception e) {
            LOG.error("Error using OpenAI, falling back to mock", e);
            return this.analyzeWithMockAI(event);
        }
    }

    /**
     * Analyzes math action using Ollama (local LLM) with conversation context.
     * Sends structured prompt and parses JSON response.
     */
    private AIFeedbackDto analyzeWithOllama(final GraspableEventDto event, final ConversationContextDto context) {
        LOG.info("Analyzing math action with Ollama");

        // Check if Ollama is available
        if (!this.ollamaService.isAvailable()) {
            LOG.warn("Ollama server not available at {}, falling back to mock AI", this.ollamaService.getApiUrl());
            return this.analyzeWithMockAI(event);
        }

        try {
            // Build the prompt with context
            final String prompt = this.buildMathTutoringPrompt(event, context);

            // Call Ollama API
            final String response = this.ollamaService.generateContent(prompt);

            // Parse response as JSON
            return this.parseFeedbackFromJSON(response);

        } catch (final Exception e) {
            LOG.error("Error using Ollama, falling back to mock", e);
            return this.analyzeWithMockAI(event);
        }
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
        final Random random = new Random();

        switch (problem.category) {
            case LINEAR_EQUATIONS:
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
                break;

            case QUADRATIC_EQUATIONS:
                // Generate simple quadratic: x^2 = n (perfect square)
                final int sqrtVal = random.nextInt(10) + 1; // 1-10
                final int nSquared = sqrtVal * sqrtVal;

                problem.title = "Solve for x";
                problem.initialExpression = String.format("x^2 = %d", nSquared);
                problem.targetExpression = String.format("x = ±%d", sqrtVal);
                problem.allowedOperations.addAll(Arrays.asList("sqrt", "simplify"));
                problem.hints.add("Take the square root of both sides");
                problem.hints.add("Remember there are two solutions: positive and negative");
                break;

            case POLYNOMIAL_SIMPLIFICATION:
                // Generate random simplification
                final int coef1 = random.nextInt(9) + 1;
                final int coef2 = random.nextInt(9) + 1;

                problem.title = "Simplify the expression";
                problem.initialExpression = String.format("%dx + %dx", coef1, coef2);
                problem.targetExpression = (coef1 + coef2) + "x";
                problem.allowedOperations.addAll(Arrays.asList("simplify", "combine"));
                problem.hints.add("Combine like terms");
                problem.hints.add("Add the coefficients of x");
                break;

            case FACTORING:
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
                break;

            case FRACTIONS:
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
                break;

            case EXPONENTS:
                // Generate exponent simplification: x^a * x^b = x^(a+b)
                final int exp1 = random.nextInt(4) + 2; // 2-5
                final int exp2 = random.nextInt(4) + 2; // 2-5

                problem.title = "Simplify using exponent rules";
                problem.initialExpression = String.format("x^%d * x^%d", exp1, exp2);
                problem.targetExpression = String.format("x^%d", exp1 + exp2);
                problem.allowedOperations.addAll(Arrays.asList("simplify", "multiply"));
                problem.hints.add("When multiplying powers with the same base, add the exponents");
                problem.hints.add(String.format("x^%d * x^%d = x^(%d+%d)", exp1, exp2, exp1, exp2));
                break;

            case SYSTEMS_OF_EQUATIONS:
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
                break;

            case INEQUALITIES:
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
                break;

            default:
                // Fallback to linear equations
                return this.generateProblem(difficulty, GraspableProblemDto.ProblemCategory.LINEAR_EQUATIONS);
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
    public void logInteraction(final GraspableEventDto event, final AIFeedbackDto feedback) {
        try {
            final var interaction = new AIInteractionEntity();
            interaction.sessionId = event.sessionId;

            // Look up user and exercise if IDs are provided
            if (event.studentId != null) {
                interaction.user = UserEntity.findById(event.studentId);
            }
            if (event.exerciseId != null) {
                interaction.exercise = ExerciseEntity.findById(event.exerciseId);
            }

            interaction.eventType = event.eventType;
            interaction.expressionBefore = event.expressionBefore;
            interaction.expressionAfter = event.expressionAfter;
            interaction.feedbackType = feedback.type.toString();
            interaction.feedbackMessage = feedback.message;
            interaction.confidenceScore = feedback.confidence;
            interaction.actionCorrect = event.correct;

            interaction.persist();
            LOG.debug("Logged AI interaction: {}", interaction.id);
        } catch (final Exception e) {
            LOG.error("Failed to log AI interaction", e);
            // Don't fail the request if logging fails
        }
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
            final var studentQuestionRecord = new AIInteractionEntity();
            studentQuestionRecord.sessionId = sessionId;
            studentQuestionRecord.eventType = "QUESTION";
            studentQuestionRecord.feedbackType = "QUESTION";
            studentQuestionRecord.studentMessage = studentQuestion;

            if (userId != null) {
                final UserEntity user = UserEntity.findById(userId);
                if (user == null) {
                    LOG.warn("User not found for logging question interaction: userId={}", userId);
                } else {
                    studentQuestionRecord.user = user;
                }
            }

            ExerciseEntity exercise = null;
            if (exerciseId != null) {
                exercise = ExerciseEntity.findById(exerciseId);
                if (exercise == null) {
                    LOG.warn("Exercise not found for logging question interaction: exerciseId={}", exerciseId);
                } else {
                    studentQuestionRecord.exercise = exercise;
                }
            }

            studentQuestionRecord.persist();
            LOG.info("Successfully logged student question: id={}, studentMessage={}",
                    studentQuestionRecord.id, studentQuestionRecord.studentMessage);

            // 2. Log the AI answer as a separate record
            final var aiAnswerRecord = new AIInteractionEntity();
            aiAnswerRecord.sessionId = sessionId;
            aiAnswerRecord.eventType = "QUESTION_ANSWER";
            aiAnswerRecord.feedbackType = "ANSWER";
            aiAnswerRecord.feedbackMessage = aiAnswer;

            if (userId != null) {
                final UserEntity user = UserEntity.findById(userId);
                if (user == null) {
                    LOG.warn("User not found for logging AI answer: userId={}", userId);
                } else {
                    aiAnswerRecord.user = user;
                }
            }

            if (exerciseId != null) {
                exercise = ExerciseEntity.findById(exerciseId);
                if (exercise == null) {
                    LOG.warn("Exercise not found for logging AI answer: exerciseId={}", exerciseId);
                } else {
                    aiAnswerRecord.exercise = exercise;
                }
            }

            aiAnswerRecord.persist();
            LOG.info("Successfully logged AI answer: id={}, feedbackMessage={}",
                    aiAnswerRecord.id, aiAnswerRecord.feedbackMessage);
        } catch (final Exception e) {
            LOG.error("Failed to log question interaction", e);
            // Don't fail the request if logging fails
        }
    }
}
