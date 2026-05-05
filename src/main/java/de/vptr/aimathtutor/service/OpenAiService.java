package de.vptr.aimathtutor.service;

import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.vptr.aimathtutor.dto.OpenAiRequestDto;
import de.vptr.aimathtutor.dto.OpenAiResponseDto;
import de.vptr.aimathtutor.service.ai.AbstractAiProviderService;
import de.vptr.aimathtutor.util.AppConstants;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Service for interacting with OpenAI Chat Completions API
 * Supports GPT-4o, gpt-5-nano, GPT-3.5-turbo, etc.
 * Configuration is loaded dynamically from AiConfigService.
 */
@ApplicationScoped
public class OpenAiService extends AbstractAiProviderService {

    private static final Logger LOG = LoggerFactory.getLogger(OpenAiService.class);
    private static final String DEFAULT_MODEL = "gpt-5-nano";
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final String CHAT_SYSTEM_PROMPT = "You are an encouraging AI math tutor helping students learn algebra. "
            + "Provide clear, supportive feedback that guides students' thinking without giving away answers.";
    private static final String JSON_SYSTEM_PROMPT = "You are an AI math tutor. Respond ONLY with valid JSON in the specified format.";

    @ConfigProperty(name = "openai.api.key", defaultValue = "")
    private String apiKey; // API key is always read from environment variable, never from database

    private volatile Client client;

    @Override
    protected String getConfigPrefix() {
        return "openai";
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
     * Get or create the JAX-RS Client with thread-safe double-checked locking.
     */
    private Client getClient() {
        Client localClient = this.client;
        if (localClient == null) {
            synchronized (this) {
                localClient = this.client;
                if (localClient == null) {
                    this.client = localClient = ClientBuilder.newBuilder()
                            .connectTimeout(10, TimeUnit.SECONDS)
                            .readTimeout(60, TimeUnit.SECONDS)
                            .build();
                    LOG.debug("Created OpenAI JAX-RS Client");
                }
            }
        }
        return localClient;
    }

    /**
     * Clean up JAX-RS client resources when the bean is destroyed.
     */
    @PreDestroy
    void cleanup() {
        final Client localClient = this.client;
        if (localClient != null) {
            synchronized (this) {
                if (this.client != null) {
                    this.client.close();
                    this.client = null;
                    LOG.debug("Closed OpenAI JAX-RS Client");
                }
            }
        }
    }

    /**
     * Generate content using OpenAI Chat Completions API
     *
     * @param prompt The user prompt
     * @return The generated text response
     */
    @Retry(maxRetries = AppConstants.RETRY_MAX_RETRIES, delay = AppConstants.RETRY_DELAY_MS, jitter = AppConstants.RETRY_JITTER_MS, abortOn = IllegalStateException.class)
    public String generateContent(final String prompt) {
        return this.doGenerate(prompt, CHAT_SYSTEM_PROMPT, false);
    }

    /**
     * Generate content with JSON mode (guarantees valid JSON)
     */
    @Retry(maxRetries = AppConstants.RETRY_MAX_RETRIES, delay = AppConstants.RETRY_DELAY_MS, jitter = AppConstants.RETRY_JITTER_MS)
    public String generateJsonContent(final String prompt) {
        return this.doGenerate(prompt, JSON_SYSTEM_PROMPT, true);
    }

    private String doGenerate(final String prompt, final String systemPrompt, final boolean jsonMode) {
        LOG.debug("Generating {} content with OpenAI for prompt length: {}",
                jsonMode ? "JSON" : "text", prompt != null ? prompt.length() : 0);

        this.requireApiKey(this.apiKey, "OPENAI_API_KEY");

        final String model = this.aiConfigService.getConfigValue("openai.model", DEFAULT_MODEL);
        final String baseUrl = this.aiConfigService.getConfigValue("openai.api.base-url", DEFAULT_BASE_URL);
        final double temperature = this.aiConfigService.getClampedTemperature("openai.temperature", 0.7);
        final int maxTokens = this.aiConfigService.getClampedTokens("openai.max-tokens", 2000);
        final String organizationId = this.aiConfigService.getConfigValue("openai.organization-id", null);

        if (model == null || model.isBlank()) {
            throw new IllegalStateException("OpenAI model not configured. Please configure via admin settings.");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("OpenAI API URL not configured. Please configure via admin settings.");
        }

        try {
            final var request = jsonMode
                    ? OpenAiRequestDto.createJsonRequest(systemPrompt, prompt, model, temperature, maxTokens)
                    : OpenAiRequestDto.createChatRequest(systemPrompt, prompt, model, temperature, maxTokens);

            final String url = baseUrl + "/chat/completions";

            Invocation.Builder requestBuilder = this.getClient().target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + this.apiKey);

            if (organizationId != null && !organizationId.isBlank()) {
                requestBuilder = requestBuilder.header("OpenAI-Organization", organizationId);
            }

            try (Response response = requestBuilder.post(Entity.json(request))) {

                if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                    final String errorBody = response.readEntity(String.class);
                    LOG.error("OpenAI API error (status {}): {}", response.getStatus(), errorBody);
                    throw new WebApplicationException("OpenAI API error: " + response.getStatus(),
                            response.getStatus());
                }

                final var openAiResponse = response.readEntity(OpenAiResponseDto.class);

                if (openAiResponse.isContentFiltered()) {
                    LOG.warn("OpenAI response was filtered by content safety policies");
                    throw new IllegalStateException("Response filtered by content safety policies");
                }

                if (openAiResponse.isTruncated()) {
                    LOG.warn("OpenAI response was truncated due to token limit");
                }

                if (!openAiResponse.isComplete()) {
                    LOG.warn("OpenAI response not complete. Finish reason: {}",
                            openAiResponse.choices != null && !openAiResponse.choices.isEmpty()
                                    ? openAiResponse.choices.get(0).finishReason
                                    : "unknown");
                }

                final String content = this.requireNonEmptyContent(openAiResponse.getTextContent());

                if (openAiResponse.usage != null) {
                    LOG.debug("OpenAI usage - Prompt: {} tokens, Completion: {} tokens, Total: {} tokens",
                            openAiResponse.usage.promptTokens,
                            openAiResponse.usage.completionTokens,
                            openAiResponse.usage.totalTokens);
                }

                LOG.debug("Successfully generated content from OpenAI, length: {}", content.length());
                return content;
            }

        } catch (final WebApplicationException e) {
            LOG.error("Error calling OpenAI API", e);
            throw e;
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error calling OpenAI API", e);
            throw new IllegalStateException("Failed to call OpenAI API", e);
        }
    }
}
