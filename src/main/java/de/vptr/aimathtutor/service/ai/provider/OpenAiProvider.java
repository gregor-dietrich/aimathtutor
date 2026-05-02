package de.vptr.aimathtutor.service.ai.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.vptr.aimathtutor.dto.AiFeedbackDto;
import de.vptr.aimathtutor.dto.ConversationContextDto;
import de.vptr.aimathtutor.dto.GraspableEventDto;
import de.vptr.aimathtutor.service.OpenAiService;
import de.vptr.aimathtutor.service.ai.JsonRepairService;
import de.vptr.aimathtutor.service.ai.PromptBuilderService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * OpenAI provider for analyzing math actions and answering questions.
 * Uses JSON mode for guaranteed valid JSON responses.
 */
@ApplicationScoped
public class OpenAiProvider implements AiProvider {

    private static final Logger LOG = LoggerFactory.getLogger(OpenAiProvider.class);

    @Inject
    OpenAiService openAiService;

    @Inject
    PromptBuilderService promptBuilderService;

    @Inject
    JsonRepairService jsonRepairService;

    @Override
    public boolean isAvailable() {
        return this.openAiService.isConfigured();
    }

    @Override
    public AiFeedbackDto analyzeMathAction(final GraspableEventDto event, final ConversationContextDto context) {
        LOG.info("Analyzing math action with OpenAI");

        final var prompt = this.promptBuilderService.buildMathTutoringPrompt(event, context);
        final var response = this.openAiService.generateJsonContent(prompt);
        return this.jsonRepairService.parseFeedbackFromJson(response);
    }

    @Override
    public String answerQuestion(final String question, final String currentExpression,
            final String initialExpression, final String targetExpression,
            final ConversationContextDto context) {
        final var prompt = this.promptBuilderService.buildQuestionAnsweringPrompt(question, currentExpression,
                initialExpression, targetExpression, context);
        return this.openAiService.generateContent(prompt);
    }
}
