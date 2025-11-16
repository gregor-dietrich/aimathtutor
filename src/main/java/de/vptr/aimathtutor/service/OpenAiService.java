package de.vptr.aimathtutor.service;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.vptr.aimathtutor.dto.OpenAiRequestDto;
import de.vptr.aimathtutor.dto.OpenAiResponseDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Service for interacting with OpenAI Chat Completions API
 * Supports GPT-4o, GPT-4o-mini, GPT-3.5-turbo, etc.
 * Configuration is loaded dynamically from AiConfigService.
 */
@ApplicationScoped
public class OpenAiService {

    private static final Logger LOG = LoggerFactory.getLogger(OpenAiService.class);

    @ConfigProperty(name = "openai.api.key", defaultValue = "")
    String apiKey; // API key is always read from environment variable, never from database

    @Inject
    AiConfigService aiConfigService;

    private Client client;

    /**
     * Generate content using OpenAI Chat Completions API
     * 
     * @param prompt The user prompt
     * @return The generated text response
     */
    public String generateContent(final String prompt) {
        LOG.debug("Generating content with OpenAI for prompt length: {}", prompt != null ? prompt.length() : 0);

        if (this.apiKey == null || this.apiKey.isBlank() || this.apiKey.startsWith("${")) {
            LOG.warn("OpenAI API key not configured");
            throw new IllegalStateException(
                    "OpenAI API key not configured. Please set OPENAI_API_KEY environment variable");
        }

        // Load dynamic configuration
        final String model = this.aiConfigService.getConfigValue("openai.model", "gpt-4o-mini");
        final String baseUrl = this.aiConfigService.getConfigValue("openai.api.base-url", "https://api.openai.com/v1");
        final Double temperature = this.aiConfigService.getConfigValueAsDouble("openai.temperature", 0.7);
        final Integer maxTokens = this.aiConfigService.getConfigValueAsInt("openai.max-tokens", 1000);
        final String organizationId = this.aiConfigService.getConfigValue("openai.organization-id", "");

        if (model == null || model.isBlank()) {
            throw new IllegalStateException("OpenAI model not configured. Please configure via admin settings.");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("OpenAI API URL not configured. Please configure via admin settings.");
        }

        try {
            // Create request with system prompt for math tutoring
            final String systemPrompt = "You are an encouraging AI math tutor helping students learn algebra. "
                    + "Provide clear, supportive feedback that guides students' thinking without giving away answers.";

            final var request = OpenAiRequestDto.createChatRequest(
                    systemPrompt,
                    prompt,
                    model,
                    temperature,
                    maxTokens);

            // Build API URL
            final String url = baseUrl + "/chat/completions";

            // Get or create client
            if (this.client == null) {
                this.client = ClientBuilder.newClient();
            }

            // Build request with headers
            var requestBuilder = this.client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + this.apiKey);

            // Add organization header if configured
            if (organizationId != null && !organizationId.isBlank()) {
                requestBuilder = requestBuilder.header("OpenAI-Organization", organizationId);
            }

            // Make API call
            final var response = requestBuilder.post(Entity.json(request));

            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                final String errorBody = response.readEntity(String.class);
                LOG.error("OpenAI API error (status {}): {}", response.getStatus(), errorBody);
                throw new WebApplicationException("OpenAI API error: " + response.getStatus(),
                        response.getStatus());
            }

            // Parse response
            final var openAiResponse = response.readEntity(OpenAiResponseDto.class);

            if (!openAiResponse.isComplete()) {
                LOG.warn("OpenAI response not complete. Finish reason: {}",
                        openAiResponse.choices != null && !openAiResponse.choices.isEmpty()
                                ? openAiResponse.choices.get(0).finishReason
                                : "unknown");
            }

            final String content = openAiResponse.getTextContent();
            if (content == null || content.isBlank()) {
                LOG.warn("OpenAI returned empty content");
                throw new IllegalStateException("Empty response from OpenAI");
            }

            // Log token usage if available
            if (openAiResponse.usage != null) {
                LOG.debug("OpenAI usage - Prompt: {} tokens, Completion: {} tokens, Total: {} tokens",
                        openAiResponse.usage.promptTokens,
                        openAiResponse.usage.completionTokens,
                        openAiResponse.usage.totalTokens);
            }

            LOG.debug("Successfully generated content from OpenAI, length: {}", content.length());
            return content;

        } catch (final WebApplicationException e) {
            LOG.error("Error calling OpenAI API", e);
            throw e;
        } catch (final Exception e) {
            LOG.error("Unexpected error calling OpenAI API", e);
            throw new IllegalStateException("Failed to call OpenAI API", e);
        }
    }

    /**
     * Generate content with JSON mode (guarantees valid JSON)
     */
    public String generateJsonContent(final String prompt) {
        LOG.debug("Generating JSON content with OpenAI for prompt length: {}", prompt != null ? prompt.length() : 0);

        if (this.apiKey == null || this.apiKey.isBlank() || this.apiKey.startsWith("${")) {
            throw new IllegalStateException("OpenAI API key not configured");
        }

        // Load dynamic configuration
        final String model = this.aiConfigService.getConfigValue("openai.model", "gpt-4o-mini");
        final String baseUrl = this.aiConfigService.getConfigValue("openai.api.base-url", "https://api.openai.com/v1");
        final Double temperature = this.aiConfigService.getConfigValueAsDouble("openai.temperature", 0.7);
        final Integer maxTokens = this.aiConfigService.getConfigValueAsInt("openai.max-tokens", 1000);
        final String organizationId = this.aiConfigService.getConfigValue("openai.organization-id", "");

        try {
            final String systemPrompt = "You are an AI math tutor. Respond ONLY with valid JSON in the specified format.";

            final var request = OpenAiRequestDto.createJsonRequest(
                    systemPrompt,
                    prompt,
                    model,
                    temperature,
                    maxTokens);

            final String url = baseUrl + "/chat/completions";

            if (this.client == null) {
                this.client = ClientBuilder.newClient();
            }

            var requestBuilder = this.client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + this.apiKey);

            if (organizationId != null && !organizationId.isBlank()) {
                requestBuilder = requestBuilder.header("OpenAI-Organization", organizationId);
            }

            final var response = requestBuilder.post(Entity.json(request));

            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                final String errorBody = response.readEntity(String.class);
                LOG.error("OpenAI API error (status {}): {}", response.getStatus(), errorBody);
                throw new WebApplicationException("OpenAI API error: " + response.getStatus(),
                        response.getStatus());
            }

            final var openAiResponse = response.readEntity(OpenAiResponseDto.class);
            final String content = openAiResponse.getTextContent();

            if (content == null || content.isBlank()) {
                throw new IllegalStateException("Empty response from OpenAI");
            }

            LOG.debug("Successfully generated JSON content from OpenAI");
            return content;

        } catch (final Exception e) {
            LOG.error("Error calling OpenAI API for JSON", e);
            throw new IllegalStateException("Failed to call OpenAI API", e);
        }
    }

    /**
     * Check if OpenAI is properly configured
     */
    public boolean isConfigured() {
        return this.apiKey != null && !this.apiKey.isBlank() && !this.apiKey.startsWith("${");
    }

    /**
     * Get the current model name
     */
    public String getModel() {
        return this.aiConfigService.getConfigValue("openai.model", "gpt-4o-mini");
    }
}
