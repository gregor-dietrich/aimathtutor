package de.vptr.aimathtutor.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.vptr.aimathtutor.dto.GeminiRequestDto;
import de.vptr.aimathtutor.dto.GeminiResponseDto;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service for interacting with Google Gemini AI API
 * Handles REST API calls to Gemini 2.5 Flash-Lite
 * Configuration is loaded dynamically from AiConfigService.
 */
@ApplicationScoped
public class GeminiService {

    private static final Logger LOG = LoggerFactory.getLogger(GeminiService.class);

    @ConfigProperty(name = "gemini.api.key", defaultValue = "")
    String apiKey; // API key is always read from environment variable, never from database

    @Inject
    AiConfigService aiConfigService;

    @Inject
    ObjectMapper objectMapper;

    private HttpClient httpClient;

    @PostConstruct
    void init() {
        // Initialize HttpClient with appropriate settings
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        LOG.debug("Initialized Gemini HttpClient");
    }

    /**
     * Generate content using Gemini API
     * 
     * @param prompt The input prompt
     * @return The generated text response
     */
    public String generateContent(final String prompt) {
        LOG.debug("Generating content with Gemini for prompt length: {}", prompt != null ? prompt.length() : 0);

        if (this.apiKey == null || this.apiKey.isBlank() || this.apiKey.startsWith("${")) {
            LOG.warn("Gemini API key not configured");
            throw new IllegalStateException(
                    "Gemini API key not configured. Please set GEMINI_API_KEY environment variable");
        }

        // Load dynamic configuration
        final String model = this.aiConfigService.getConfigValue("gemini.model", "gemini-2.5-flash-lite");
        final String baseUrl = this.aiConfigService.getConfigValue("gemini.api.base-url",
                "https://generativelanguage.googleapis.com");
        final Double temperature = this.aiConfigService.getConfigValueAsDouble("gemini.temperature", 0.7);
        final Integer maxTokens = this.aiConfigService.getConfigValueAsInt("gemini.max-tokens", 2000);

        if (model == null || model.isBlank()) {
            throw new IllegalStateException("Gemini model not configured. Please configure via admin settings.");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("Gemini API URL not configured. Please configure via admin settings.");
        }

        try {
            // Create request DTO
            final var requestDto = GeminiRequestDto.createTextRequest(prompt, temperature, maxTokens);

            // Convert to JSON
            final String requestJson = this.objectMapper.writeValueAsString(requestDto);

            // Build API URL
            final String url = String.format("%s/v1beta/models/%s:generateContent?key=%s",
                    baseUrl, model, this.apiKey);

            LOG.debug("Calling Gemini API at: {}", url.replaceAll("key=[^&]+", "key=***"));

            // Create HTTP request
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            // Make API call
            final HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            final int statusCode = response.statusCode();
            final String responseBody = response.body();

            if (statusCode != 200) {
                LOG.error("Gemini API error (status {}): {}", statusCode, responseBody);
                throw new IllegalStateException("Gemini API error: " + statusCode + " - " + responseBody);
            }

            // Parse response
            final var geminiResponse = this.objectMapper.readValue(responseBody, GeminiResponseDto.class);

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

        } catch (final IOException | InterruptedException e) {
            LOG.error("Error calling Gemini API", e);
            throw new IllegalStateException("Failed to call Gemini API", e);
        }
    }

    /**
     * Check if Gemini is properly configured
     */
    public boolean isConfigured() {
        return this.apiKey != null && !this.apiKey.isBlank() && !this.apiKey.startsWith("${");
    }

    /**
     * Get the current model name
     */
    public String getModel() {
        return this.aiConfigService.getConfigValue("gemini.model", "gemini-2.5-flash-lite");
    }
}
