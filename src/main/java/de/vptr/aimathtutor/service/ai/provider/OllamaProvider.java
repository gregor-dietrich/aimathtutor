package de.vptr.aimathtutor.service.ai.provider;

import org.jboss.logging.Logger;

import de.vptr.aimathtutor.dto.AiFeedbackDto;
import de.vptr.aimathtutor.dto.ConversationContextDto;
import de.vptr.aimathtutor.dto.GraspableEventDto;
import de.vptr.aimathtutor.service.ai.OllamaService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Ollama AI provider for analyzing math actions and answering questions.
 */
@ApplicationScoped
public class OllamaProvider extends AbstractProvider {

    private static final Logger LOG = Logger.getLogger(OllamaProvider.class);

    @Inject
    OllamaService ollamaService;

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
    protected String generateContent(final String prompt) {
        return this.ollamaService.generateContent(prompt);
    }
}
