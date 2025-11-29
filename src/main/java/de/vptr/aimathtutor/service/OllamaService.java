package de.vptr.aimathtutor.service;

import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.vptr.aimathtutor.dto.OllamaRequestDto;
import de.vptr.aimathtutor.dto.OllamaResponseDto;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
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
public class OllamaService {

    private static final Logger LOG = LoggerFactory.getLogger(OllamaService.class);

    @Inject
    AiConfigService aiConfigService;

    @Inject
    @ConfigProperty(name = "ollama.client.connect-timeout-seconds", defaultValue = "10")
    int connectTimeoutSeconds;

    @Inject
    @ConfigProperty(name = "ollama.client.read-timeout-seconds", defaultValue = "60")
    int readTimeoutSeconds;

    private volatile Client client;

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
    public String generateContent(final String prompt) {
        LOG.debug("Generating content with Ollama for prompt length: {}", prompt != null ? prompt.length() : 0);

        // Load dynamic configuration
        final String apiUrl = this.aiConfigService.getConfigValue("ollama.api.url", "http://ollama:11434");
        final String model = this.aiConfigService.getConfigValue("ollama.model", "llama3.2:3b");
        final Double temperature = this.aiConfigService.getConfigValueAsDouble("ollama.temperature", 0.7);
        final Integer maxTokens = this.aiConfigService.getConfigValueAsInt("ollama.max-tokens", 1000);

        if (apiUrl == null || apiUrl.isBlank()) {
            throw new IllegalStateException("Ollama API URL not configured. Please configure via admin settings.");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalStateException("Ollama model not configured. Please configure via admin settings.");
        }

        try {
            // Create request
            final var request = OllamaRequestDto.createGenerateRequest(prompt, model, temperature, maxTokens);

            // Build API URL
            final String url = apiUrl + "/api/generate";

            // Make API call
            final long startTime = System.currentTimeMillis();
            final var response = this.getClient().target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(request));

            final long duration = System.currentTimeMillis() - startTime;

            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                final String errorBody = response.readEntity(String.class);
                LOG.error("Ollama API error (status {}): {}", response.getStatus(), errorBody);
                throw new WebApplicationException("Ollama API error: " + response.getStatus(),
                        response.getStatus());
            }

            // Parse response
            final var ollamaResponse = response.readEntity(OllamaResponseDto.class);

            if (!ollamaResponse.isComplete()) {
                LOG.warn("Ollama response not complete");
            }

            final String content = ollamaResponse.getTextContent();
            if (content == null || content.isBlank()) {
                LOG.warn("Ollama returned empty content");
                throw new IllegalStateException("Empty response from Ollama");
            }

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

        } catch (final WebApplicationException e) {
            LOG.error("Error calling Ollama API", e);
            throw e;
        } catch (final Exception e) {
            LOG.error("Unexpected error calling Ollama API", e);
            throw new IllegalStateException("Failed to call Ollama API: " + e.getMessage(), e);
        }
    }

    /**
     * Check if Ollama server is available
     */
    public boolean isAvailable() {
        final String apiUrl = this.aiConfigService.getConfigValue("ollama.api.url", "http://ollama:11434");
        try {
            // Check /api/tags endpoint (lists installed models)
            final var response = this.getClient().target(apiUrl + "/api/tags")
                    .request(MediaType.APPLICATION_JSON)
                    .get();

            final boolean available = response.getStatus() == Response.Status.OK.getStatusCode();

            if (available) {
                LOG.debug("Ollama server is available at {}", apiUrl);
            } else {
                LOG.debug("Ollama server not available at {} (status: {})", apiUrl, response.getStatus());
            }

            return available;

        } catch (final Exception e) {
            LOG.debug("Ollama server not available at {}: {}", apiUrl, e.getMessage());
            return false;
        }
    }

    /**
     * Check if a specific model is installed
     */
    public boolean isModelInstalled(final String modelName) {
        final String apiUrl = this.aiConfigService.getConfigValue("ollama.api.url", "http://ollama:11434");
        try {
            final var response = this.getClient().target(apiUrl + "/api/tags")
                    .request(MediaType.APPLICATION_JSON)
                    .get();

            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                return false;
            }

            final String body = response.readEntity(String.class);
            // Simple check if model name appears in response
            return body.contains(modelName);

        } catch (final Exception e) {
            LOG.debug("Error checking if model {} is installed: {}", modelName, e.getMessage());
            return false;
        }
    }

    /**
     * Check if Ollama is properly configured
     */
    public boolean isConfigured() {
        return this.isAvailable();
    }

    /**
     * Get the current model name
     */
    public String getModel() {
        return this.aiConfigService.getConfigValue("ollama.model", "llama3.2:3b");
    }

    /**
     * Get the API URL
     */
    public String getApiUrl() {
        return this.aiConfigService.getConfigValue("ollama.api.url", "http://ollama:11434");
    }
}
