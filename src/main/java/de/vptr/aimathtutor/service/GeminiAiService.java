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
 */
@ApplicationScoped
public class GeminiAiService {

    private static final Logger LOG = LoggerFactory.getLogger(GeminiAiService.class);

    @ConfigProperty(name = "gemini.api.key")
    String apiKey;

    @ConfigProperty(name = "gemini.model", defaultValue = "gemini-2.5-flash-lite")
    String model;

    @ConfigProperty(name = "gemini.api.base-url", defaultValue = "https://generativelanguage.googleapis.com")
    String baseUrl;

    @ConfigProperty(name = "gemini.temperature", defaultValue = "0.7")
    Double temperature;

    @ConfigProperty(name = "gemini.max-tokens", defaultValue = "1000")
    Integer maxTokens;

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

        LOG.debug("Initialized Gemini HttpClient for model: {}", this.model);
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
                    "Gemini API key not configured. Please set gemini.api.key in application.properties or GEMINI_API_KEY environment variable");
        }

        try {
            // Create request DTO
            final var requestDto = GeminiRequestDto.createTextRequest(prompt, this.temperature, this.maxTokens);

            // Convert to JSON
            final String requestJson = this.objectMapper.writeValueAsString(requestDto);

            // Build API URL
            final String url = String.format("%s/v1beta/models/%s:generateContent?key=%s",
                    this.baseUrl, this.model, this.apiKey);

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
     * Generate content with custom temperature and max tokens
     */
    public String generateContent(final String prompt, final Double customTemperature, final Integer customMaxTokens) {
        final Double originalTemp = this.temperature;
        final Integer originalMaxTokens = this.maxTokens;

        try {
            this.temperature = customTemperature;
            this.maxTokens = customMaxTokens;
            return this.generateContent(prompt);
        } finally {
            this.temperature = originalTemp;
            this.maxTokens = originalMaxTokens;
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
        return this.model;
    }
}
