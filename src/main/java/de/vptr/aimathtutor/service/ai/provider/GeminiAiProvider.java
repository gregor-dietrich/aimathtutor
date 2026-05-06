package de.vptr.aimathtutor.service.ai.provider;

import org.jboss.logging.Logger;

import de.vptr.aimathtutor.dto.AiFeedbackDto;
import de.vptr.aimathtutor.dto.ConversationContextDto;
import de.vptr.aimathtutor.dto.GraspableEventDto;
import de.vptr.aimathtutor.service.GeminiService;
import de.vptr.aimathtutor.service.ai.JsonRepairService;
import de.vptr.aimathtutor.service.ai.PromptBuilderService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Gemini AI provider for analyzing math actions and answering questions.
 */
@ApplicationScoped
public class GeminiAiProvider implements AiProvider {

    private static final Logger LOG = Logger.getLogger(GeminiAiProvider.class);

    @Inject
    GeminiService geminiService;

    @Inject
    PromptBuilderService promptBuilderService;

    @Inject
    JsonRepairService jsonRepairService;

    @Override
    public boolean isAvailable() {
        return this.geminiService.isConfigured();
    }

    @Override
    public AiFeedbackDto analyzeMathAction(final GraspableEventDto event, final ConversationContextDto context) {
        LOG.info("Analyzing math action with Gemini AI");

        final var prompt = this.promptBuilderService.buildMathTutoringPrompt(event, context);
        final var response = this.geminiService.generateContent(prompt);
        return this.jsonRepairService.parseFeedbackFromJson(response);
    }

    @Override
    public String answerQuestion(final String question, final String currentExpression,
            final String initialExpression, final String targetExpression,
            final ConversationContextDto context) {
        final var prompt = this.promptBuilderService.buildQuestionAnsweringPrompt(question, currentExpression,
                initialExpression, targetExpression, context);
        return this.geminiService.generateContent(prompt);
    }
}
