package de.vptr.aimathtutor.service;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.vptr.aimathtutor.dto.GeminiRequestDto;
import de.vptr.aimathtutor.dto.GeminiResponseDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Service for interacting with Google Gemini AI API
 * Handles REST API calls to Gemini 1.5 Flash
 */
@ApplicationScoped
public class GeminiAIService {

    private static final Logger LOG = LoggerFactory.getLogger(GeminiAIService.class);

    @ConfigProperty(name = "gemini.api.key")
    String apiKey;

    @ConfigProperty(name = "gemini.model", defaultValue = "gemini-1.5-flash")
    String model;

    @ConfigProperty(name = "gemini.api.base-url", defaultValue = "https://generativelanguage.googleapis.com/v1beta")
    String baseUrl;

    @ConfigProperty(name = "gemini.temperature", defaultValue = "0.7")
    Double temperature;

    @ConfigProperty(name = "gemini.max-tokens", defaultValue = "1000")
    Integer maxTokens;

    @Inject
    ObjectMapper objectMapper;

    private Client client;

    /**
     * Generate content using Gemini API
     * 
     * @param prompt The input prompt
     * @return The generated text response
     */
    public String generateContent(String prompt) {
        LOG.debug("Generating content with Gemini for prompt length: {}", prompt != null ? prompt.length() : 0);

        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("${")) {
            LOG.warn("Gemini API key not configured");
            throw new IllegalStateException(
                    "Gemini API key not configured. Please set gemini.api.key in application.properties or GEMINI_API_KEY environment variable");
        }

        try {
            // Create request
            final var request = GeminiRequestDto.createTextRequest(prompt, temperature, maxTokens);

            // Build API URL
            final String url = String.format("%s/models/%s:generateContent?key=%s", baseUrl, model, apiKey);

            // Get or create client
            if (client == null) {
                client = ClientBuilder.newClient();
            }

            // Make API call
            final var response = client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(request));

            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                final String errorBody = response.readEntity(String.class);
                LOG.error("Gemini API error (status {}): {}", response.getStatus(), errorBody);
                throw new WebApplicationException("Gemini API error: " + response.getStatus(), response.getStatus());
            }

            // Parse response
            final var geminiResponse = response.readEntity(GeminiResponseDto.class);

            if (geminiResponse.isBlocked()) {
                LOG.warn("Gemini response was blocked by safety filters");
                throw new IllegalStateException("Response blocked by safety filters");
            }

            final String content = geminiResponse.getTextContent();
            if (content == null || content.isBlank()) {
                LOG.warn("Gemini returned empty content");
                throw new IllegalStateException("Empty response from Gemini");
            }

            LOG.debug("Successfully generated content from Gemini, length: {}", content.length());
            return content;

        } catch (final WebApplicationException e) {
            LOG.error("Error calling Gemini API", e);
            throw e;
        } catch (final Exception e) {
            LOG.error("Unexpected error calling Gemini API", e);
            throw new IllegalStateException("Failed to call Gemini API", e);
        }
    }

    /**
     * Generate content with custom temperature and max tokens
     */
    public String generateContent(String prompt, Double customTemperature, Integer customMaxTokens) {
        final Double originalTemp = this.temperature;
        final Integer originalMaxTokens = this.maxTokens;

        try {
            this.temperature = customTemperature;
            this.maxTokens = customMaxTokens;
            return generateContent(prompt);
        } finally {
            this.temperature = originalTemp;
            this.maxTokens = originalMaxTokens;
        }
    }

    /**
     * Check if Gemini is properly configured
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
