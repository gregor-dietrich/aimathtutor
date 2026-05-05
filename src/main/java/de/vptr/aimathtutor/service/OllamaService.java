package de.vptr.aimathtutor.service;

import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.vptr.aimathtutor.dto.OllamaRequestDto;
import de.vptr.aimathtutor.dto.OllamaResponseDto;
import de.vptr.aimathtutor.dto.OllamaTagsResponseDto;
import de.vptr.aimathtutor.service.ai.AbstractAiProviderService;
import de.vptr.aimathtutor.service.ai.AiConfigKeys;
import de.vptr.aimathtutor.service.ai.AiProviderException;
import de.vptr.aimathtutor.service.ai.NonRetryableAiProviderException;
import de.vptr.aimathtutor.util.AppConstants;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Service for interacting with Ollama local LLM API
 * Supports phi4, qwen3, deepseek, and other Ollama models
 * Configuration is loaded dynamically from AiConfigService.
 */
@ApplicationScoped
public class OllamaService extends AbstractAiProviderService {

    private static final Logger LOG = LoggerFactory.getLogger(OllamaService.class);
    private static final String DEFAULT_MODEL = "llama3.2:3b";
    private static final String DEFAULT_API_URL = "http://ollama:11434";

    @Inject
    @ConfigProperty(name = "ollama.client.connect-timeout-seconds", defaultValue = "10")
    int connectTimeoutSeconds;

    @Inject
    @ConfigProperty(name = "ollama.client.read-timeout-seconds", defaultValue = "60")
    int readTimeoutSeconds;

    private volatile Client client;

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

    /**
     * Ollama is configured when the server is reachable; no API key is required.
     */
    @Override
    public boolean isConfigured() {
        return this.isAvailable();
    }

    /**
     * Get or create the JAX-RS client with thread-safe lazy initialization.
     * This avoids creating the client if Ollama is not the active AI provider.
     *
     * @return The configured JAX-RS Client instance
     */
    private synchronized Client getClient() {
        if (this.client == null) {
            this.client = ClientBuilder.newBuilder()
                    .connectTimeout(this.connectTimeoutSeconds, TimeUnit.SECONDS)
                    .readTimeout(this.readTimeoutSeconds, TimeUnit.SECONDS)
                    .build();
            LOG.debug("Initialized Ollama JAX-RS Client (connectTimeout={}s, readTimeout={}s)",
                    this.connectTimeoutSeconds, this.readTimeoutSeconds);
        }
        return this.client;
    }

    /**
     * Clean up resources when the bean is destroyed.
     * Synchronized to ensure consistent access to the client field.
     */
    @PreDestroy
    synchronized void cleanup() {
        if (this.client != null) {
            this.client.close();
            this.client = null;
            LOG.debug("Closed Ollama JAX-RS Client");
        }
    }

    /**
     * Generate content using Ollama Generate API
     *
     * @param prompt The input prompt
     * @return The generated text response
     */
    @Retry(maxRetries = AppConstants.RETRY_MAX_RETRIES, delay = AppConstants.RETRY_DELAY_MS, jitter = AppConstants.RETRY_JITTER_MS, abortOn = NonRetryableAiProviderException.class)
    public String generateContent(final String prompt) {
        LOG.debug("Generating content with Ollama for prompt length: {}", prompt != null ? prompt.length() : 0);

        // Load dynamic configuration
        final String apiUrl = this.aiConfigService.getConfigValue(AiConfigKeys.OLLAMA_API_URL, DEFAULT_API_URL);
        final String model = this.aiConfigService.getConfigValue(AiConfigKeys.OLLAMA_MODEL, DEFAULT_MODEL);
        final double temperature = this.aiConfigService.getClampedTemperature(AiConfigKeys.OLLAMA_TEMPERATURE, 0.7);
        // Default to 2000 tokens to prevent truncated JSON responses
        final int maxTokens = this.aiConfigService.getClampedTokens(AiConfigKeys.OLLAMA_MAX_TOKENS, 2000);

        this.requireConfigured(apiUrl, "Ollama API URL");
        this.requireConfigured(model, "Ollama model");

        try {
            // Create request
            final var request = OllamaRequestDto.createGenerateRequest(prompt, model, temperature, maxTokens);

            // Build API URL
            final String url = apiUrl + "/api/generate";

            // Make API call
            final long startTime = System.currentTimeMillis();
            try (Response response = this.getClient().target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(request))) {

                final long duration = System.currentTimeMillis() - startTime;

                if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                    final String errorBody = response.readEntity(String.class);
                    LOG.error("Ollama API error (status {}): {}", response.getStatus(), errorBody);
                    throw AiProviderException.httpFailure(this.getProviderName(), response.getStatus(), errorBody);
                }

                // Parse response
                final var ollamaResponse = response.readEntity(OllamaResponseDto.class);

                if (!ollamaResponse.isComplete()) {
                    LOG.warn("Ollama response not complete");
                }

                if (ollamaResponse.isTruncated()) {
                    LOG.warn("Ollama response was truncated due to max-tokens limit (done_reason={})",
                            ollamaResponse.doneReason);
                }

                final String content = this.requireNonEmptyContent(ollamaResponse.getTextContent());

                // Log performance metrics
                final Double tokensPerSecond = ollamaResponse.getTokensPerSecond();
                if (tokensPerSecond != null) {
                    LOG.debug("Ollama generated {} tokens at {} tokens/second in {}ms",
                            ollamaResponse.evalCount,
                            String.format("%.2f", tokensPerSecond),
                            duration);
                } else {
                    LOG.debug("Successfully generated content from Ollama in {}ms, length: {}", duration,
                            content.length());
                }

                return content;
            }

        } catch (final AiProviderException e) {
            LOG.error("Ollama provider call failed", e);
            throw e;
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error calling Ollama API", e);
            throw AiProviderException.transportFailure(this.getProviderName(),
                    "Failed to call Ollama API: " + e.getMessage(), e);
        }
    }

    /**
     * Check if Ollama server is available
     */
    public boolean isAvailable() {
        final String apiUrl = this.aiConfigService.getConfigValue(AiConfigKeys.OLLAMA_API_URL, DEFAULT_API_URL);
        try {
            // Check /api/tags endpoint (lists installed models)
            try (Response response = this.getClient().target(apiUrl + "/api/tags")
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {

                final boolean available = response.getStatus() == Response.Status.OK.getStatusCode();

                if (available) {
                    LOG.debug("Ollama server is available at {}", apiUrl);
                } else {
                    LOG.debug("Ollama server not available at {} (status: {})", apiUrl, response.getStatus());
                }

                return available;
            }

        } catch (final RuntimeException e) {
            LOG.debug("Ollama server not available at {}: {}", apiUrl, e.getMessage());
            return false;
        }
    }

    /**
     * Check if a specific model is installed
     */
    public boolean isModelInstalled(final String modelName) {
        final String apiUrl = this.aiConfigService.getConfigValue(AiConfigKeys.OLLAMA_API_URL, DEFAULT_API_URL);
        try {
            try (Response response = this.getClient().target(apiUrl + "/api/tags")
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {

                if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                    return false;
                }

                final var tagsResponse = response.readEntity(OllamaTagsResponseDto.class);
                if (tagsResponse.models != null) {
                    return tagsResponse.models.stream()
                            .anyMatch(m -> modelName.equals(m.name) || modelName.equals(m.model));
                }
                return false;
            }

        } catch (final RuntimeException e) {
            LOG.debug("Error checking if model {} is installed: {}", modelName, e.getMessage());
            return false;
        }
    }

    /**
     * Get the API URL
     */
    public String getApiUrl() {
        return this.aiConfigService.getConfigValue(AiConfigKeys.OLLAMA_API_URL, DEFAULT_API_URL);
    }
}
