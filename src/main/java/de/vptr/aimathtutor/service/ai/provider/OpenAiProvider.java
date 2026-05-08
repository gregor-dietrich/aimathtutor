package de.vptr.aimathtutor.service.ai.provider;

import org.jboss.logging.Logger;

import de.vptr.aimathtutor.dto.AiFeedbackDto;
import de.vptr.aimathtutor.dto.ConversationContextDto;
import de.vptr.aimathtutor.dto.GraspableEventDto;
import de.vptr.aimathtutor.service.OpenAiService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * OpenAI provider for analyzing math actions and answering questions. Uses JSON mode for guaranteed valid JSON
 * responses.
 */
@ApplicationScoped
public class OpenAiProvider extends AbstractAiProvider {

    private static final Logger LOG = Logger.getLogger(OpenAiProvider.class);

    @Inject
    OpenAiService openAiService;

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
    protected String generateContent(final String prompt) {
        return this.openAiService.generateContent(prompt);
    }
}
