package de.vptr.aimathtutor.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.vptr.aimathtutor.dto.GeminiRequestDto;
import de.vptr.aimathtutor.dto.GeminiResponseDto;
import de.vptr.aimathtutor.service.ai.AbstractAiProviderService;
import de.vptr.aimathtutor.service.ai.AiConfigKeys;
import de.vptr.aimathtutor.service.ai.AiProviderException;
import de.vptr.aimathtutor.service.ai.NonRetryableAiProviderException;
import de.vptr.aimathtutor.util.AppConstants;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service for interacting with Google Gemini AI API
 * Handles REST API calls to Gemini 2.5 Flash-Lite
 * Configuration is loaded dynamically from AiConfigService.
 */
@ApplicationScoped
public class GeminiService extends AbstractAiProviderService {

    private static final Logger LOG = Logger.getLogger(GeminiService.class);
    private static final String DEFAULT_MODEL = "gemma-3-27b-it";
    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com";

    @ConfigProperty(name = "gemini.api.key", defaultValue = "")
    private String apiKey; // API key is always read from environment variable, never from database

    @Inject
    ObjectMapper objectMapper;

    private HttpClient httpClient;

    @Override
    protected String getConfigPrefix() {
        return AiConfigKeys.GEMINI_PREFIX;
    }

    @Override
    protected String getDefaultModel() {
        return DEFAULT_MODEL;
    }

    @Override
    protected String getProviderName() {
        return "Gemini";
    }

    @Override
    public boolean isConfigured() {
        return isApiKeyConfigured(this.apiKey);
    }

    @PostConstruct
    void init() {
        // Initialize HttpClient with appropriate settings
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        LOG.debug("Initialized Gemini HttpClient");
    }

    /**
     * Generate content using Gemini API
     *
     * @param prompt The input prompt
     * @return The generated text response
     */
    @Retry(maxRetries = AppConstants.RETRY_MAX_RETRIES, delay = AppConstants.RETRY_DELAY_MS, jitter = AppConstants.RETRY_JITTER_MS, abortOn = NonRetryableAiProviderException.class)
    public String generateContent(final String prompt) {
        LOG.debugf("Generating content with Gemini for prompt length: %s",  prompt != null ? prompt.length() : 0);

        this.requireApiKey(this.apiKey, "GEMINI_API_KEY");

        // Load dynamic configuration
        final String model = this.aiConfigService.getConfigValue(AiConfigKeys.GEMINI_MODEL, DEFAULT_MODEL);
        final String baseUrl = this.aiConfigService.getConfigValue(AiConfigKeys.GEMINI_API_BASE_URL, DEFAULT_BASE_URL);
        final double temperature = this.aiConfigService.getClampedTemperature(AiConfigKeys.GEMINI_TEMPERATURE, 0.7);
        final int maxTokens = this.aiConfigService.getClampedTokens(AiConfigKeys.GEMINI_MAX_TOKENS, 2000);

        this.requireConfigured(model, "Gemini model");
        this.requireConfigured(baseUrl, "Gemini API URL");

        try {
            // Create request DTO
            final var requestDto = GeminiRequestDto.createTextRequest(prompt, temperature, maxTokens);

            // Convert to JSON
            final String requestJson = this.objectMapper.writeValueAsString(requestDto);

            // Build API URL (key moved to header to avoid appearing in logs/proxies)
            final String url = String.format("%s/v1beta/models/%s:generateContent",
                    baseUrl, model);

            LOG.debugf("Calling Gemini API at: %s",  url);

            // Create HTTP request with API key in header instead of query param
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", this.apiKey)
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            // Make API call
            final HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            final int statusCode = response.statusCode();
            final String responseBody = response.body();

            if (statusCode != 200) {
                LOG.errorf("Gemini API error (status %s): %s",  statusCode,  responseBody);
                throw AiProviderException.httpFailure(this.getProviderName(), statusCode, responseBody);
            }

            // Parse response
            final var geminiResponse = this.objectMapper.readValue(responseBody, GeminiResponseDto.class);

            if (geminiResponse.isBlocked()) {
                LOG.warn("Gemini response was blocked by safety filters");
                throw new NonRetryableAiProviderException(this.getProviderName(),
                        "Response blocked by safety filters");
            }

            if (geminiResponse.isTruncated()) {
                LOG.warnf("Gemini response was truncated due to token limit (finishReason=%s)", 
                        geminiResponse.getFinishReason());
            }

            final String content = this.requireNonEmptyContent(geminiResponse.getTextContent());

            LOG.debugf("Successfully generated content from Gemini, length: %s",  content.length());
            return content;

        } catch (final AiProviderException e) {
            LOG.error("Gemini provider call failed", e);
            throw e;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Gemini call interrupted", e);
            throw AiProviderException.transportFailure(this.getProviderName(), "Call interrupted", e);
        } catch (final IOException e) {
            LOG.error("Error calling Gemini API", e);
            throw AiProviderException.transportFailure(this.getProviderName(), "Failed to call Gemini API", e);
        }
    }
}
