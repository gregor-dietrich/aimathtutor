package de.vptr.aimathtutor.service;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.vptr.aimathtutor.dto.OpenAIRequestDto;
import de.vptr.aimathtutor.dto.OpenAIResponseDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Service for interacting with OpenAI Chat Completions API
 * Supports GPT-4o, GPT-4o-mini, GPT-3.5-turbo, etc.
 */
@ApplicationScoped
public class OpenAIService {

    private static final Logger LOG = LoggerFactory.getLogger(OpenAIService.class);

    @ConfigProperty(name = "openai.api.key")
    String apiKey;

    @ConfigProperty(name = "openai.model", defaultValue = "gpt-4o-mini")
    String model;

    @ConfigProperty(name = "openai.api.base-url", defaultValue = "https://api.openai.com/v1")
    String baseUrl;

    @ConfigProperty(name = "openai.temperature", defaultValue = "0.7")
    Double temperature;

    @ConfigProperty(name = "openai.max-tokens", defaultValue = "1000")
    Integer maxTokens;

    @ConfigProperty(name = "openai.organization-id")
    String organizationId;

    private Client client;

    /**
     * Generate content using OpenAI Chat Completions API
     * 
     * @param prompt The user prompt
     * @return The generated text response
     */
    public String generateContent(String prompt) {
        LOG.debug("Generating content with OpenAI for prompt length: {}", prompt != null ? prompt.length() : 0);

        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("${")) {
            LOG.warn("OpenAI API key not configured");
            throw new IllegalStateException(
                    "OpenAI API key not configured. Please set openai.api.key in application.properties or OPENAI_API_KEY environment variable");
        }

        try {
            // Create request with system prompt for math tutoring
            final String systemPrompt = "You are an encouraging AI math tutor helping students learn algebra. "
                    + "Provide clear, supportive feedback that guides students' thinking without giving away answers.";

            final var request = OpenAIRequestDto.createChatRequest(
                    systemPrompt,
                    prompt,
                    model,
                    temperature,
                    maxTokens);

            // Build API URL
            final String url = baseUrl + "/chat/completions";

            // Get or create client
            if (client == null) {
                client = ClientBuilder.newClient();
            }

            // Build request with headers
            var requestBuilder = client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey);

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
            final var openAIResponse = response.readEntity(OpenAIResponseDto.class);

            if (!openAIResponse.isComplete()) {
                LOG.warn("OpenAI response not complete. Finish reason: {}",
                        openAIResponse.choices != null && !openAIResponse.choices.isEmpty()
                                ? openAIResponse.choices.get(0).finishReason
                                : "unknown");
            }

            final String content = openAIResponse.getTextContent();
            if (content == null || content.isBlank()) {
                LOG.warn("OpenAI returned empty content");
                throw new IllegalStateException("Empty response from OpenAI");
            }

            // Log token usage if available
            if (openAIResponse.usage != null) {
                LOG.debug("OpenAI usage - Prompt: {} tokens, Completion: {} tokens, Total: {} tokens",
                        openAIResponse.usage.promptTokens,
                        openAIResponse.usage.completionTokens,
                        openAIResponse.usage.totalTokens);
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
    public String generateJsonContent(String prompt) {
        LOG.debug("Generating JSON content with OpenAI for prompt length: {}", prompt != null ? prompt.length() : 0);

        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("${")) {
            throw new IllegalStateException("OpenAI API key not configured");
        }

        try {
            final String systemPrompt = "You are an AI math tutor. Respond ONLY with valid JSON in the specified format.";

            final var request = OpenAIRequestDto.createJsonRequest(
                    systemPrompt,
                    prompt,
                    model,
                    temperature,
                    maxTokens);

            final String url = baseUrl + "/chat/completions";

            if (client == null) {
                client = ClientBuilder.newClient();
            }

            var requestBuilder = client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey);

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

            final var openAIResponse = response.readEntity(OpenAIResponseDto.class);
            final String content = openAIResponse.getTextContent();

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
        return apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("${");
    }

    /**
     * Get the current model name
     */
    public String getModel() {
        return model;
    }
}
