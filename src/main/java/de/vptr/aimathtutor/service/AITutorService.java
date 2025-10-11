package de.vptr.aimathtutor.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Random;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.vptr.aimathtutor.dto.AIFeedbackDto;
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
     * 
     * @param event The Graspable Math event containing the student's action
     * @return AI-generated feedback
     */
    public AIFeedbackDto analyzeMathAction(final GraspableEventDto event) {
        LOG.debug("Analyzing math action: {}", event);

        if (this.aiEnabled != null && !this.aiEnabled) {
            return AIFeedbackDto.positive("Keep going!");
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
     * Mock AI implementation using rule-based logic.
     * Provides reasonable feedback based on action type.
     */
    private AIFeedbackDto analyzeWithMockAI(final GraspableEventDto event) {
        final AIFeedbackDto feedback;

        // Analyze based on event type
        switch (event.eventType != null ? event.eventType.toLowerCase() : "") {
            case "simplify":
                if (event.correct != null && event.correct) {
                    feedback = AIFeedbackDto.positive("Great job! You simplified the expression correctly.");
                    feedback.suggestedNextSteps.add("Continue simplifying if possible");
                } else if (event.correct != null && !event.correct) {
                    feedback = AIFeedbackDto.corrective("Hmm, that simplification doesn't look quite right.");
                    feedback.hints.add("Check if you applied the operation to both sides");
                    feedback.hints.add("Remember the order of operations");
                } else {
                    // No correctness info provided
                    feedback = AIFeedbackDto.positive("You're simplifying the expression.");
                    feedback.suggestedNextSteps.add("Continue simplifying if possible");
                }
                break;

            case "expand":
                feedback = AIFeedbackDto.positive("Good! You expanded the expression.");
                feedback.suggestedNextSteps.add("Now try to simplify the result");
                feedback.relatedConcepts.add("Distributive property");
                break;

            case "factor":
                feedback = AIFeedbackDto.positive("Nice factoring!");
                feedback.relatedConcepts.add("Factoring");
                feedback.relatedConcepts.add("Common factors");
                break;

            case "combine":
                feedback = AIFeedbackDto
                        .suggestion("You're combining like terms. Make sure the terms are actually alike!");
                feedback.hints.add("Like terms have the same variable and exponent");
                break;

            case "move":
                feedback = AIFeedbackDto.hint("Moving terms across the equals sign? Remember to change the sign!");
                feedback.relatedConcepts.add("Equation balancing");
                break;

            default:
                feedback = AIFeedbackDto.neutral("I see you're working on the problem. Keep it up!");
                break;
        }

        feedback.sessionId = event.sessionId;
        feedback.confidence = 0.85; // Mock confidence

        return feedback;
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

        prompt.append("You are an encouraging AI math tutor helping a student learn algebra. ");
        prompt.append("Analyze the student's action and provide helpful feedback.\n\n");

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
        prompt.append("  \"message\": \"Your encouraging feedback message (one sentence)\",\n");
        prompt.append("  \"hints\": [\"optional hint 1\", \"optional hint 2\"],\n");
        prompt.append("  \"suggestedNextSteps\": [\"what to try next\"],\n");
        prompt.append("  \"confidence\": 0.0 to 1.0\n");
        prompt.append("}\n\n");

        prompt.append("Guidelines:\n");
        prompt.append("- Be encouraging and supportive\n");
        prompt.append("- If the action is correct, praise the student\n");
        prompt.append("- If incorrect, gently point out the error without giving away the answer\n");
        prompt.append("- Provide hints that guide thinking, don't solve it for them\n");
        prompt.append("- Suggest logical next steps\n");
        prompt.append("- Keep messages concise (1-2 sentences)\n");

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
