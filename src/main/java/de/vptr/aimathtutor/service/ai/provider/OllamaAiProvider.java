package de.vptr.aimathtutor.service.ai.provider;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.vptr.aimathtutor.dto.AiFeedbackDto;
import de.vptr.aimathtutor.dto.ConversationContextDto;
import de.vptr.aimathtutor.dto.GraspableEventDto;
import de.vptr.aimathtutor.service.OllamaService;
import de.vptr.aimathtutor.service.ai.JsonRepairService;
import de.vptr.aimathtutor.service.ai.PromptBuilderService;
import de.vptr.aimathtutor.util.AppConstants;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Ollama AI provider for analyzing math actions and answering questions.
 * Uses MicroProfile Fault Tolerance {@code @Retry} for automatic retries with
 * jitter.
 */
@ApplicationScoped
public class OllamaAiProvider implements AiProvider {

    private static final Logger LOG = LoggerFactory.getLogger(OllamaAiProvider.class);

    @Inject
    OllamaService ollamaService;

    @Inject
    PromptBuilderService promptBuilderService;

    @Inject
    JsonRepairService jsonRepairService;

    @Override
    public boolean isAvailable() {
        return this.ollamaService.isAvailable();
    }

    @Override
    public AiFeedbackDto analyzeMathAction(final GraspableEventDto event, final ConversationContextDto context) {
        LOG.info("Analyzing math action with Ollama");
        final var prompt = this.promptBuilderService.buildMathTutoringPrompt(event, context);
        final var response = this.ollamaService.generateContent(prompt);
        return this.jsonRepairService.parseFeedbackFromJson(response);
    }

    @Override
    public String answerQuestion(final String question, final String currentExpression,
            final String initialExpression, final String targetExpression,
            final ConversationContextDto context) {
        final var prompt = this.promptBuilderService.buildQuestionAnsweringPrompt(question, currentExpression,
                initialExpression, targetExpression, context);
        return this.ollamaService.generateContent(prompt);
    }

    /**
     * Internal method to call Ollama for math action analysis with retry support.
     * <p>
     * NOTE: This method is public so that CDI proxies can intercept it and apply
     * the {@code @Retry} annotation. When called through the injected proxy,
     * MicroProfile Fault Tolerance will work correctly.
     */
    @Retry(maxRetries = AppConstants.RETRY_MAX_RETRIES, delay = AppConstants.RETRY_DELAY_MS, jitter = AppConstants.RETRY_JITTER_MS)
    public AiFeedbackDto callOllamaForAnalysis(final GraspableEventDto event, final ConversationContextDto context) {
        return this.analyzeMathAction(event, context);
    }

    /**
     * Internal method to call Ollama for question answering with retry support.
     * <p>
     * NOTE: This method is public so that CDI proxies can intercept it and apply
     * the {@code @Retry} annotation. When called through the injected proxy,
     * MicroProfile Fault Tolerance will work correctly.
     */
    @Retry(maxRetries = AppConstants.RETRY_MAX_RETRIES, delay = AppConstants.RETRY_DELAY_MS, jitter = AppConstants.RETRY_JITTER_MS)
    public String callOllamaForQuestion(final String question, final String currentExpression,
            final String initialExpression, final String targetExpression,
            final ConversationContextDto context) {
        return this.answerQuestion(question, currentExpression, initialExpression, targetExpression, context);
    }
}
