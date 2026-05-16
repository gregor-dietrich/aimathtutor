package de.vptr.aimathtutor.service.ai;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.logging.Logger;

import de.vptr.aimathtutor.dto.OpenAiRequestDto;
import de.vptr.aimathtutor.dto.OpenAiResponseDto;
import de.vptr.aimathtutor.exception.NonRetryableProviderException;
import de.vptr.aimathtutor.exception.ProviderException;
import de.vptr.aimathtutor.util.AppConstants;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Service for interacting with OpenAI Chat Completions API Supports GPT-4o, gpt-5-nano, GPT-3.5-turbo, etc.
 * Configuration is loaded dynamically from AiConfigService.
 */
@ApplicationScoped
public class OpenAiService extends AbstractProviderService {

    private static final Logger LOG = Logger.getLogger(OpenAiService.class);
    private static final String DEFAULT_MODEL = "gpt-5-nano";
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final String CHAT_SYSTEM_PROMPT =
            "You are an encouraging AI math tutor helping students learn algebra. "
                    + "Provide clear, supportive feedback that guides students' thinking without giving away answers.";
    private static final String JSON_SYSTEM_PROMPT =
            "You are an AI math tutor. Respond ONLY with valid JSON in the specified format.";

    @ConfigProperty(name = "app.openai.api.key", defaultValue = "")
    @Nullable
    private String apiKey; // API key is always read from environment variable, never from database

    @Override
    protected String getConfigPrefix() {
        return AiConfigKeys.OPENAI_PREFIX;
    }

    @Override
    protected String getDefaultModel() {
        return DEFAULT_MODEL;
    }

    @Override
    protected String getProviderName() {
        return "OpenAI";
    }

    @Override
    public boolean isConfigured() {
        return isApiKeyConfigured(this.apiKey);
    }

    /**
     * Generate content using OpenAI Chat Completions API
     *
     * @param prompt
     *            The user prompt
     * @return The generated text response
     */
    @Retry(maxRetries = AppConstants.RETRY_MAX_RETRIES, delay = AppConstants.RETRY_DELAY_MS,
            jitter = AppConstants.RETRY_JITTER_MS, abortOn = NonRetryableProviderException.class)
    public String generateContent(final String prompt) {
        return this.doGenerate(prompt, CHAT_SYSTEM_PROMPT, false);
    }

    /**
     * Generate content with JSON mode (guarantees valid JSON)
     */
    @Retry(maxRetries = AppConstants.RETRY_MAX_RETRIES, delay = AppConstants.RETRY_DELAY_MS,
            jitter = AppConstants.RETRY_JITTER_MS, abortOn = NonRetryableProviderException.class)
    public String generateJsonContent(final String prompt) {
        return this.doGenerate(prompt, JSON_SYSTEM_PROMPT, true);
    }

    private String doGenerate(final String prompt, final String systemPrompt, final boolean jsonMode) {
        LOG.debugf("Generating %s content with OpenAI for prompt length: %s", jsonMode ? "JSON" : "text",
                prompt != null ? prompt.length() : 0);

        this.requireApiKey(this.apiKey, "app.openai.api.key");

        final String model = this.aiConfigService.getConfigValue(AiConfigKeys.OPENAI_MODEL, DEFAULT_MODEL);
        final String baseUrl = this.aiConfigService.getConfigValue(AiConfigKeys.OPENAI_API_BASE_URL, DEFAULT_BASE_URL);
        final double temperature = this.aiConfigService.getClampedTemperature(AiConfigKeys.OPENAI_TEMPERATURE, 0.7);
        final int maxTokens = this.aiConfigService.getClampedTokens(AiConfigKeys.OPENAI_MAX_TOKENS, 2000);
        final String organizationId = this.aiConfigService.getConfigValue(AiConfigKeys.OPENAI_ORGANIZATION_ID, null);

        this.requireConfigured(model, "OpenAI model");
        this.requireConfigured(baseUrl, "OpenAI API URL");

        // The following checks satisfy NullAway; requireConfigured already threw if null
        if (model == null || baseUrl == null) {
            return "";
        }

        final var requestDto =
                jsonMode ? OpenAiRequestDto.createJsonRequest(systemPrompt, prompt, model, temperature, maxTokens)
                        : OpenAiRequestDto.createChatRequest(systemPrompt, prompt, model, temperature, maxTokens);

        try {
            Invocation.Builder builder = this.getClient().target(baseUrl).path("/chat/completions")
                    .request(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + this.apiKey);
            if (organizationId != null && !organizationId.isBlank()) {
                builder = builder.header("OpenAI-Organization", organizationId);
            }
            try (Response response = builder.post(Entity.json(requestDto))) {
                final int status = response.getStatus();
                if (status != 200) {
                    final String errorBody = response.readEntity(String.class);
                    LOG.errorf("OpenAI API error (status %s): %s", status, errorBody);
                    throw ProviderException.httpFailure(this.getProviderName(), status, errorBody);
                }

                final var responseDto = response.readEntity(OpenAiResponseDto.class);
                final String content = responseDto.getTextContent();

                return this.requireNonEmptyContent(content);
            }
        } catch (final ProviderException e) {
            throw e;
        } catch (final Exception e) {
            LOG.error("Error calling OpenAI API", e);
            throw ProviderException.transportFailure(this.getProviderName(), "Failed to call OpenAI API", e);
        }
    }
}
