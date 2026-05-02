package de.vptr.aimathtutor.service.ai.provider;

import de.vptr.aimathtutor.dto.AiFeedbackDto;
import de.vptr.aimathtutor.dto.ConversationContextDto;
import de.vptr.aimathtutor.dto.GraspableEventDto;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Mock AI provider using rule-based logic.
 * Provides reasonable feedback based on action type without calling external
 * services.
 */
@ApplicationScoped
public class MockAiProvider implements AiProvider {

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public AiFeedbackDto analyzeMathAction(final GraspableEventDto event, final ConversationContextDto context) {
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

    @Override
    public String answerQuestion(final String question, final String currentExpression,
            final String initialExpression, final String targetExpression,
            final ConversationContextDto context) {
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
            return "\uD83D\uDCA1 Look for terms you can combine, or operations you can undo to isolate the variable.";
        }

        return "I'm here to help! Can you be more specific about what you're stuck on?";
    }
}
