package de.vptr.aimathtutor.service;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.vptr.aimathtutor.dto.OllamaRequestDto;
import de.vptr.aimathtutor.dto.OllamaResponseDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Service for interacting with Ollama local LLM API
 * Supports llama3.1, qwen2.5, phi3, deepseek-coder, and other Ollama models
 */
@ApplicationScoped
public class OllamaService {

    private static final Logger LOG = LoggerFactory.getLogger(OllamaService.class);

    @ConfigProperty(name = "ollama.api.url", defaultValue = "http://localhost:11434")
    String apiUrl;

    @ConfigProperty(name = "ollama.model", defaultValue = "llama3.1:8b")
    String model;

    @ConfigProperty(name = "ollama.temperature", defaultValue = "0.7")
    Double temperature;

    @ConfigProperty(name = "ollama.max-tokens", defaultValue = "1000")
    Integer maxTokens;

    @ConfigProperty(name = "ollama.timeout-seconds", defaultValue = "60")
    Integer timeoutSeconds;

    private Client client;

    /**
     * Generate content using Ollama Generate API
     * 
     * @param prompt The input prompt
     * @return The generated text response
     */
    public String generateContent(final String prompt) {
        LOG.debug("Generating content with Ollama for prompt length: {}", prompt != null ? prompt.length() : 0);

        try {
            // Check if Ollama is available
            if (!this.isAvailable()) {
                LOG.warn("Ollama server not available at {}", this.apiUrl);
                throw new IllegalStateException(
                        "Ollama server not available. Please ensure Ollama is running at " + this.apiUrl);
            }

            // Create request
            final var request = OllamaRequestDto.createGenerateRequest(prompt, this.model, this.temperature,
                    this.maxTokens);

            // Build API URL
            final String url = this.apiUrl + "/api/generate";

            // Get or create client
            if (this.client == null) {
                this.client = ClientBuilder.newClient();
            }

            // Make API call
            final long startTime = System.currentTimeMillis();
            final var response = this.client.target(url)
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
        try {
            if (this.client == null) {
                this.client = ClientBuilder.newClient();
            }

            // Check /api/tags endpoint (lists installed models)
            final var response = this.client.target(this.apiUrl + "/api/tags")
                    .request(MediaType.APPLICATION_JSON)
                    .get();

            final boolean available = response.getStatus() == Response.Status.OK.getStatusCode();

            if (available) {
                LOG.debug("Ollama server is available at {}", this.apiUrl);
            } else {
                LOG.debug("Ollama server not available at {} (status: {})", this.apiUrl, response.getStatus());
            }

            return available;

        } catch (final Exception e) {
            LOG.debug("Ollama server not available at {}: {}", this.apiUrl, e.getMessage());
            return false;
        }
    }

    /**
     * Check if a specific model is installed
     */
    public boolean isModelInstalled(final String modelName) {
        try {
            if (this.client == null) {
                this.client = ClientBuilder.newClient();
            }

            final var response = this.client.target(this.apiUrl + "/api/tags")
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
        return this.model;
    }

    /**
     * Get the API URL
     */
    public String getApiUrl() {
        return this.apiUrl;
    }
}
