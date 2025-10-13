package de.vptr.aimathtutor.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Random;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.vptr.aimathtutor.dto.AIFeedbackDto;
import de.vptr.aimathtutor.dto.ChatMessageDto;
import de.vptr.aimathtutor.dto.GraspableEventDto;
import de.vptr.aimathtutor.dto.GraspableProblemDto;
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
     * @param event The Graspable Math event containing the student's action
     * @return AI-generated feedback, or null if no feedback needed
     */
    public AIFeedbackDto analyzeMathAction(final GraspableEventDto event) {
        LOG.debug("Analyzing math action: {}", event);

        if (this.aiEnabled != null && !this.aiEnabled) {
            return null; // Don't provide feedback if AI is disabled
        }

        // Filter out insignificant actions to reduce spam
        if (!this.isSignificantAction(event)) {
            LOG.debug("Skipping feedback for insignificant action: {}", event.eventType);
            return null;
        }

        // Use different AI provider based on configuration
        final String provider = (this.aiProvider != null) ? this.aiProvider.toLowerCase() : "mock";
        return switch (provider) {
            case "gemini" -> this.analyzeWithGemini(event);
            case "openai" -> this.analyzeWithOpenAI(event);
            case "ollama" -> this.analyzeWithOllama(event);
            default -> this.analyzeWithMockAI(event);
        };
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

        // Significant actions that deserve feedback
        return type.contains("simplify") ||
                type.contains("expand") ||
                type.contains("factor") ||
                type.contains("solve") ||
                type.contains("combine") ||
                type.contains("isolate") ||
                type.contains("substitute") ||
                (type.contains("move") && event.expressionBefore != null && event.expressionAfter != null
                        && !event.expressionBefore.equals(event.expressionAfter));
    }

    /**
     * Answers a direct question from the student.
     * Uses AI to provide contextual help based on the current problem state.
     * 
     * @param question          The student's question
     * @param currentExpression The current state of the problem (optional)
     * @param sessionId         The session ID (optional)
     * @return AI-generated answer
     */
    public ChatMessageDto answerQuestion(final String question, final String currentExpression,
            final String sessionId) {
        LOG.debug("Answering question: {} (session: {})", question, sessionId);

        if (this.aiEnabled != null && !this.aiEnabled) {
            return ChatMessageDto.aiAnswer(
                    "I'm currently offline, but keep working on the problem! You can ask your teacher for help.");
        }

        // Use different AI provider based on configuration
        final String provider = (this.aiProvider != null) ? this.aiProvider.toLowerCase() : "mock";

        final String answer = switch (provider) {
            case "gemini" -> this.answerWithGemini(question, currentExpression);
            case "openai" -> this.answerWithOpenAI(question, currentExpression);
            case "ollama" -> this.answerWithOllama(question, currentExpression);
            default -> this.answerWithMockAI(question, currentExpression);
        };

        final var message = ChatMessageDto.aiAnswer(answer);
        message.sessionId = sessionId;
        return message;
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
     * Answer question using Gemini AI.
     */
    private String answerWithGemini(final String question, final String currentExpression) {
        if (!this.geminiService.isConfigured()) {
            LOG.warn("Gemini not configured, using mock AI");
            return this.answerWithMockAI(question, currentExpression);
        }

        try {
            final String prompt = this.buildQuestionAnsweringPrompt(question, currentExpression);
            return this.geminiService.generateContent(prompt);
        } catch (final Exception e) {
            LOG.error("Error using Gemini for question answering", e);
            return this.answerWithMockAI(question, currentExpression);
        }
    }

    /**
     * Answer question using OpenAI.
     */
    private String answerWithOpenAI(final String question, final String currentExpression) {
        if (!this.openAIService.isConfigured()) {
            LOG.warn("OpenAI not configured, using mock AI");
            return this.answerWithMockAI(question, currentExpression);
        }

        try {
            final String prompt = this.buildQuestionAnsweringPrompt(question, currentExpression);
            return this.openAIService.generateContent(prompt);
        } catch (final Exception e) {
            LOG.error("Error using OpenAI for question answering", e);
            return this.answerWithMockAI(question, currentExpression);
        }
    }

    /**
     * Answer question using Ollama.
     */
    private String answerWithOllama(final String question, final String currentExpression) {
        if (!this.ollamaService.isAvailable()) {
            LOG.warn("Ollama not available, using mock AI");
            return this.answerWithMockAI(question, currentExpression);
        }

        try {
            final String prompt = this.buildQuestionAnsweringPrompt(question, currentExpression);
            return this.ollamaService.generateContent(prompt);
        } catch (final Exception e) {
            LOG.error("Error using Ollama for question answering", e);
            return this.answerWithMockAI(question, currentExpression);
        }
    }

    /**
     * Builds a prompt for answering student questions.
     */
    private String buildQuestionAnsweringPrompt(final String question, final String currentExpression) {
        final StringBuilder prompt = new StringBuilder();

        prompt.append(
                "You are a helpful AI math tutor. A student is working on an algebra problem and has asked you a question.\n\n");

        if (currentExpression != null && !currentExpression.trim().isEmpty()) {
            prompt.append("Current problem state: ").append(currentExpression).append("\n\n");
        }

        prompt.append("Student question: ").append(question).append("\n\n");

        prompt.append("Provide a helpful, encouraging answer that:\n");
        prompt.append("- Guides the student's thinking without solving it for them\n");
        prompt.append("- Is concise (2-3 sentences max)\n");
        prompt.append("- Relates to their current problem if possible\n");
        prompt.append("- Uses clear, simple language\n");
        prompt.append("- Encourages them to try the next step\n\n");

        prompt.append("Your answer:");

        return prompt.toString();
    }

    /**
     * Analyzes math action using Google Gemini AI.
     * Sends a structured prompt and parses JSON response.
     */
    private AIFeedbackDto analyzeWithGemini(final GraspableEventDto event) {
        LOG.info("Analyzing math action with Gemini AI");

        // Check if Gemini is configured
        if (!this.geminiService.isConfigured()) {
            LOG.warn("Gemini not configured, falling back to mock AI");
            return this.analyzeWithMockAI(event);
        }

        try {
            // Build the prompt
            final String prompt = this.buildMathTutoringPrompt(event);

            // Call Gemini API
            final String response = this.geminiService.generateContent(prompt);

            // Parse response as JSON
            return this.parseFeedbackFromJSON(response);

        } catch (final Exception e) {
            LOG.error("Error using Gemini AI, falling back to mock", e);
            return this.analyzeWithMockAI(event);
        }
    }

    /**
     * Builds a structured prompt for math tutoring with Gemini.
     */
    private String buildMathTutoringPrompt(final GraspableEventDto event) {
        final StringBuilder prompt = new StringBuilder();

        prompt.append("You are an encouraging but concise AI math tutor helping a student learn algebra. ");
        prompt.append("Analyze the student's action and provide brief, helpful feedback.\n\n");

        prompt.append("Student Action:\n");
        prompt.append("- Action Type: ").append(event.eventType != null ? event.eventType : "unknown").append("\n");

        if (event.expressionBefore != null) {
            prompt.append("- Expression Before: ").append(event.expressionBefore).append("\n");
        }

        if (event.expressionAfter != null) {
            prompt.append("- Expression After: ").append(event.expressionAfter).append("\n");
        }

        if (event.correct != null) {
            prompt.append("- Is Correct: ").append(event.correct).append("\n");
        }

        prompt.append("\nProvide feedback in the following JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"type\": \"POSITIVE\" or \"CORRECTIVE\" or \"HINT\" or \"SUGGESTION\",\n");
        prompt.append("  \"message\": \"Your brief, encouraging feedback (ONE sentence only)\",\n");
        prompt.append("  \"hints\": [],\n");
        prompt.append("  \"suggestedNextSteps\": [],\n");
        prompt.append("  \"confidence\": 0.0 to 1.0\n");
        prompt.append("}\n\n");

        prompt.append("IMPORTANT Guidelines:\n");
        prompt.append("- Keep message to ONE SHORT sentence (max 15 words)\n");
        prompt.append("- Be encouraging but not overly enthusiastic\n");
        prompt.append("- If the action is correct, give brief praise\n");
        prompt.append("- If incorrect, point out the error gently\n");
        prompt.append("- Only provide hints array if student made a mistake (max 1-2 hints)\n");
        prompt.append("- Do NOT provide hints for correct actions\n");
        prompt.append("- Leave suggestedNextSteps empty unless specifically needed\n");
        prompt.append("- Be specific about what they did, not generic\n");

        return prompt.toString();
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
     * Analyzes math action using OpenAI (GPT models).
     * Uses JSON mode for guaranteed valid JSON responses.
     */
    private AIFeedbackDto analyzeWithOpenAI(final GraspableEventDto event) {
        LOG.info("Analyzing math action with OpenAI");

        // Check if OpenAI is configured
        if (!this.openAIService.isConfigured()) {
            LOG.warn("OpenAI not configured, falling back to mock AI");
            return this.analyzeWithMockAI(event);
        }

        try {
            // Build the prompt
            final String prompt = this.buildMathTutoringPrompt(event);

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
     * Analyzes math action using Ollama (local LLM).
     * Sends structured prompt and parses JSON response.
     */
    private AIFeedbackDto analyzeWithOllama(final GraspableEventDto event) {
        LOG.info("Analyzing math action with Ollama");

        // Check if Ollama is available
        if (!this.ollamaService.isAvailable()) {
            LOG.warn("Ollama server not available at {}, falling back to mock AI", this.ollamaService.getApiUrl());
            return this.analyzeWithMockAI(event);
        }

        try {
            // Build the prompt
            final String prompt = this.buildMathTutoringPrompt(event);

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
     * @param topic      The math topic (e.g., "algebra", "equations")
     * @return A new Graspable Math problem
     */
    public GraspableProblemDto generateProblem(final String difficulty, final String topic) {
        LOG.debug("Generating problem: difficulty={}, topic={}", difficulty, topic);

        final var problem = new GraspableProblemDto();
        problem.difficulty = difficulty;

        // Generate random problems based on topic
        final Random random = new Random();

        switch (topic != null ? topic.toLowerCase() : "algebra") {
            case "algebra":
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

            case "factoring":
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

            default:
                // Generate random simplification
                final int coef1 = random.nextInt(9) + 1;
                final int coef2 = random.nextInt(9) + 1;

                problem.title = "Simplify the expression";
                problem.initialExpression = String.format("%dx + %dx", coef1, coef2);
                problem.targetExpression = (coef1 + coef2) + "x";
                problem.allowedOperations.addAll(Arrays.asList("simplify", "combine"));
                break;
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
}
