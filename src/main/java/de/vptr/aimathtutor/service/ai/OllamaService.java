package de.vptr.aimathtutor.service.ai;

import java.util.Objects;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.logging.Logger;

import de.vptr.aimathtutor.dto.OllamaRequestDto;
import de.vptr.aimathtutor.dto.OllamaResponseDto;
import de.vptr.aimathtutor.dto.OllamaTagsResponseDto;
import de.vptr.aimathtutor.exception.NonRetryableProviderException;
import de.vptr.aimathtutor.exception.ProviderException;
import de.vptr.aimathtutor.util.AppConstants;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Service for interacting with Ollama local LLM API Supports phi4, qwen3, deepseek, and other Ollama models
 * Configuration is loaded dynamically from AiConfigService.
 */
@ApplicationScoped
public class OllamaService extends AbstractProviderService {

    private static final Logger LOG = Logger.getLogger(OllamaService.class);
    private static final String DEFAULT_MODEL = "llama3.2:3b";
    private static final String DEFAULT_API_URL = "http://ollama:11434";

    @Inject
    @ConfigProperty(name = "ollama.client.connect-timeout-seconds", defaultValue = "10")
    int connectTimeoutSeconds;

    @Inject
    @ConfigProperty(name = "ollama.client.read-timeout-seconds", defaultValue = "60")
    int readTimeoutSeconds;

    @Override
    protected String getConfigPrefix() {
        return AiConfigKeys.OLLAMA_PREFIX;
    }

    @Override
    protected String getDefaultModel() {
        return DEFAULT_MODEL;
    }

    @Override
    protected String getProviderName() {
        return "Ollama";
    }

    @Override
    protected int getConnectTimeoutSeconds() {
        return this.connectTimeoutSeconds;
    }

    @Override
    protected int getReadTimeoutSeconds() {
        return this.readTimeoutSeconds;
    }

    /**
     * Ollama is configured when the server is reachable; no API key is required.
     */
    @Override
    public boolean isConfigured() {
        return this.isAvailable();
    }

    /**
     * Generate content using Ollama Generate API
     *
     * @param prompt
     *            The input prompt
     * @return The generated text response
     */
    @Retry(maxRetries = AppConstants.RETRY_MAX_RETRIES, delay = AppConstants.RETRY_DELAY_MS,
            jitter = AppConstants.RETRY_JITTER_MS, abortOn = NonRetryableProviderException.class)
    public String generateContent(final String prompt) {
        LOG.debugf("Generating content with Ollama for prompt length: %s", prompt != null ? prompt.length() : 0);

        // Load dynamic configuration
        final String apiUrl = this.aiConfigService.getConfigValue(AiConfigKeys.OLLAMA_API_URL, DEFAULT_API_URL);
        final String model = this.aiConfigService.getConfigValue(AiConfigKeys.OLLAMA_MODEL, DEFAULT_MODEL);
        final double temperature = this.aiConfigService.getClampedTemperature(AiConfigKeys.OLLAMA_TEMPERATURE, 0.7);
        // Default to 2000 tokens to prevent truncated JSON responses
        final int maxTokens = this.aiConfigService.getClampedTokens(AiConfigKeys.OLLAMA_MAX_TOKENS, 2000);

        this.requireConfigured(apiUrl, "Ollama API URL");
        this.requireConfigured(model, "Ollama model");

        final var effectiveModel = Objects.requireNonNull(model);
        final var effectiveApiUrl = Objects.requireNonNull(apiUrl);
        this.requireSafeProviderUrl(effectiveApiUrl, AiConfigService.ProviderType.OLLAMA);

        try {
            // Create request
            final var request = OllamaRequestDto.createGenerateRequest(prompt, effectiveModel, temperature, maxTokens);

            // Build API URL
            final String url = effectiveApiUrl + "/api/generate";

            // Make API call
            final long startTime = System.currentTimeMillis();
            try (Response response =
                    this.getClient().target(url).request(MediaType.APPLICATION_JSON).post(Entity.json(request))) {

                final long duration = System.currentTimeMillis() - startTime;

                if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                    final String errorBody = response.readEntity(String.class);
                    LOG.errorf("Ollama API error (status %s): %s", response.getStatus(), errorBody);
                    throw ProviderException.httpFailure(this.getProviderName(), response.getStatus(), errorBody);
                }

                // Parse response
                final var ollamaResponse = response.readEntity(OllamaResponseDto.class);

                if (!ollamaResponse.isComplete()) {
                    LOG.warn("Ollama response not complete");
                }

                if (ollamaResponse.isTruncated()) {
                    LOG.warnf("Ollama response was truncated due to max-tokens limit (done_reason=%s)",
                            ollamaResponse.doneReason);
                }

                final String content = this.requireNonEmptyContent(ollamaResponse.getTextContent());

                // Log performance metrics
                final Double tokensPerSecond = ollamaResponse.getTokensPerSecond();
                if (tokensPerSecond != null) {
                    LOG.debugf("Ollama generated %s tokens at %s tokens/second in %sms",
                            (Object) ollamaResponse.evalCount, String.format("%.2f", tokensPerSecond), duration);
                } else {
                    LOG.debugf("Successfully generated content from Ollama in %sms, length: %s", duration,
                            content.length());
                }

                return content;
            }

        } catch (final ProviderException e) {
            LOG.error("Ollama provider call failed", e);
            throw e;
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error calling Ollama API", e);
            throw ProviderException.transportFailure(this.getProviderName(),
                    "Failed to call Ollama API: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the configured Ollama API URL after SSRF validation, or {@code null} when the URL is missing or fails the
     * allow-list. Shared by health-check paths so they can short-circuit without issuing an HTTP request.
     */
    @Nullable
    private String resolveValidatedApiUrl() {
        final String apiUrl = this.aiConfigService.getConfigValue(AiConfigKeys.OLLAMA_API_URL, DEFAULT_API_URL);
        if (apiUrl == null || apiUrl.isBlank()) {
            return null;
        }
        try {
            this.aiConfigService.validateProviderApiUrl(apiUrl, AiConfigService.ProviderType.OLLAMA);
            return apiUrl;
        } catch (final IllegalArgumentException e) {
            LOG.debugf("Ollama URL rejected by SSRF guard: %s", e.getMessage());
            return null;
        }
    }

    /**
     * Check if Ollama server is available
     */
    public boolean isAvailable() {
        final String apiUrl = this.resolveValidatedApiUrl();
        if (apiUrl == null) {
            return false;
        }
        try {
            // Check /api/tags endpoint (lists installed models)
            try (Response response =
                    this.getClient().target(apiUrl + "/api/tags").request(MediaType.APPLICATION_JSON).get()) {

                final boolean available = response.getStatus() == Response.Status.OK.getStatusCode();

                if (available) {
                    LOG.debugf("Ollama server is available at %s", apiUrl);
                } else {
                    LOG.debugf("Ollama server not available at %s (status: %s)", apiUrl, response.getStatus());
                }

                return available;
            }

        } catch (final RuntimeException e) {
            LOG.debugf(e, "Ollama server not available at %s: %s", apiUrl, e.getMessage());
            return false;
        }
    }

    /**
     * Check if a specific model is installed
     */
    public boolean isModelInstalled(final String modelName) {
        final String apiUrl = this.resolveValidatedApiUrl();
        if (apiUrl == null) {
            return false;
        }
        try {
            try (Response response =
                    this.getClient().target(apiUrl + "/api/tags").request(MediaType.APPLICATION_JSON).get()) {

                if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                    return false;
                }

                final var tagsResponse = response.readEntity(OllamaTagsResponseDto.class);
                return tagsResponse.models != null && tagsResponse.models.stream()
                        .anyMatch(m -> modelName.equals(m.name) || modelName.equals(m.model));
            }

        } catch (final RuntimeException e) {
            LOG.debugf("Error checking if model %s is installed: %s", modelName, e.getMessage());
            return false;
        }
    }

    /**
     * Get the API URL
     */
    @Nullable
    public String getApiUrl() {
        return this.aiConfigService.getConfigValue(AiConfigKeys.OLLAMA_API_URL, DEFAULT_API_URL);
    }
}
