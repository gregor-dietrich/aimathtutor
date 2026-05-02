package de.vptr.aimathtutor.service.ai.provider;

import de.vptr.aimathtutor.dto.AiFeedbackDto;
import de.vptr.aimathtutor.dto.ConversationContextDto;
import de.vptr.aimathtutor.dto.GraspableEventDto;

/**
 * Strategy interface for AI providers that analyze math actions and answer
 * student questions.
 */
public interface AiProvider {

    /**
     * Checks whether this AI provider is available and configured.
     *
     * @return true if the provider can be used
     */
    boolean isAvailable();

    /**
     * Analyzes a student's math action and provides AI feedback.
     *
     * @param event   the math action event
     * @param context conversation context
     * @return AI-generated feedback
     */
    AiFeedbackDto analyzeMathAction(GraspableEventDto event, ConversationContextDto context);

    /**
     * Answers a direct question from the student.
     *
     * @param question          the student's question
     * @param currentExpression the current math expression
     * @param initialExpression the original problem state
     * @param targetExpression  the target solution state
     * @param context           conversation context
     * @return the AI's answer
     */
    String answerQuestion(String question, String currentExpression, String initialExpression,
            String targetExpression, ConversationContextDto context);
}
