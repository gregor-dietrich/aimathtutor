package de.vptr.aimathtutor.service.ai.provider;

import de.vptr.aimathtutor.dto.ConversationContextDto;
import de.vptr.aimathtutor.service.ai.JsonRepairService;
import de.vptr.aimathtutor.service.ai.PromptBuilderService;
import jakarta.inject.Inject;

/**
 * Base class for AI providers sharing common prompt-building and question-answering logic.
 */
public abstract class AbstractAiProvider implements AiProvider {

    @Inject
    protected PromptBuilderService promptBuilderService;

    @Inject
    protected JsonRepairService jsonRepairService;

    /**
     * Generates content from the given prompt using the provider-specific service.
     *
     * @param prompt
     *            the prompt to send to the AI
     * @return the generated content
     */
    protected abstract String generateContent(String prompt);

    @Override
    public String answerQuestion(final String question, final String currentExpression, final String initialExpression,
            final String targetExpression, final ConversationContextDto context) {
        final var prompt = this.promptBuilderService.buildQuestionAnsweringPrompt(question, currentExpression,
                initialExpression, targetExpression, context);
        return this.generateContent(prompt);
    }
}
