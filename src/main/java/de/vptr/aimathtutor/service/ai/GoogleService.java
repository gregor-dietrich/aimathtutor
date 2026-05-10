package de.vptr.aimathtutor.service.ai;

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

import de.vptr.aimathtutor.dto.GoogleRequestDto;
import de.vptr.aimathtutor.dto.GoogleResponseDto;
import de.vptr.aimathtutor.exception.NonRetryableProviderException;
import de.vptr.aimathtutor.exception.ProviderException;
import de.vptr.aimathtutor.util.AppConstants;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service for interacting with the Google AI API. Configuration is provided dynamically via {@link AiConfigService}.
 */
@ApplicationScoped
public class GoogleService extends AbstractProviderService {

    private static final Logger LOG = Logger.getLogger(GoogleService.class);
    private static final String DEFAULT_MODEL = "gemma-4-31b-it";
    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com";

    @ConfigProperty(name = "google.api.key", defaultValue = "")
    @Nullable
    private String apiKey; // API key is always read from environment variable, never from database

    @Inject
    ObjectMapper objectMapper;

    private HttpClient httpClient;

    @Override
    protected String getConfigPrefix() {
        return AiConfigKeys.GOOGLE_PREFIX;
    }

    @Override
    protected String getDefaultModel() {
        return DEFAULT_MODEL;
    }

    @Override
    protected String getProviderName() {
        return "Google";
    }

    @Override
    public boolean isConfigured() {
        return isApiKeyConfigured(this.apiKey);
    }

    @PostConstruct
    void init() {
        // Initialize HttpClient with appropriate settings
        this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10)).build();

        LOG.debug("Initialized Google HttpClient");
    }

    /**
     * Generate content using Google API
     *
     * @param prompt
     *            The input prompt
     * @return The generated text response
     */
    @Retry(maxRetries = AppConstants.RETRY_MAX_RETRIES, delay = AppConstants.RETRY_DELAY_MS,
            jitter = AppConstants.RETRY_JITTER_MS, abortOn = NonRetryableProviderException.class)
    public String generateContent(final String prompt) {
        LOG.debugf("Generating content with Google for prompt length: %s", prompt != null ? prompt.length() : 0);

        this.requireApiKey(this.apiKey, "GOOGLE_API_KEY");

        // Load dynamic configuration
        final String model = this.aiConfigService.getConfigValue(AiConfigKeys.GOOGLE_MODEL, DEFAULT_MODEL);
        final String baseUrl = this.aiConfigService.getConfigValue(AiConfigKeys.GOOGLE_API_BASE_URL, DEFAULT_BASE_URL);
        final double temperature = this.aiConfigService.getClampedTemperature(AiConfigKeys.GOOGLE_TEMPERATURE, 0.7);
        final int maxTokens = this.aiConfigService.getClampedTokens(AiConfigKeys.GOOGLE_MAX_TOKENS, 2000);

        this.requireConfigured(model, "Google model");
        this.requireConfigured(baseUrl, "Google API URL");

        try {
            // Create request DTO
            final var requestDto = GoogleRequestDto.createTextRequest(prompt, temperature, maxTokens);

            // Convert to JSON
            final String requestJson = this.objectMapper.writeValueAsString(requestDto);

            // Build API URL (key moved to header to avoid appearing in logs/proxies)
            final String url = String.format("%s/v1beta/models/%s:generateContent", baseUrl, model);

            LOG.debugf("Calling Google API at: %s", url);

            // Create HTTP request with API key in header instead of query param
            final HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
                    .header("Content-Type", "application/json").header("x-goog-api-key", this.apiKey)
                    .timeout(Duration.ofSeconds(60)).POST(HttpRequest.BodyPublishers.ofString(requestJson)).build();

            // Make API call
            final HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            final int statusCode = response.statusCode();
            final String responseBody = response.body();

            if (statusCode != 200) {
                LOG.errorf("Google API error (status %s): %s", statusCode, responseBody);
                throw ProviderException.httpFailure(this.getProviderName(), statusCode, responseBody);
            }

            // Parse response
            final var googleResponse = this.objectMapper.readValue(responseBody, GoogleResponseDto.class);

            if (googleResponse.isBlocked()) {
                LOG.warn("Google response was blocked by safety filters");
                throw new NonRetryableProviderException(this.getProviderName(), "Response blocked by safety filters");
            }

            if (googleResponse.isTruncated()) {
                LOG.warnf("Google response was truncated due to token limit (finishReason=%s)",
                        googleResponse.getFinishReason());
            }

            final String content = this.requireNonEmptyContent(googleResponse.getTextContent());

            LOG.debugf("Successfully generated content from Google, length: %s", content.length());
            return content;

        } catch (final ProviderException e) {
            LOG.error("Google provider call failed", e);
            throw e;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Google call interrupted", e);
            throw ProviderException.transportFailure(this.getProviderName(), "Call interrupted", e);
        } catch (final IOException e) {
            LOG.error("Error calling Google API", e);
            throw ProviderException.transportFailure(this.getProviderName(), "Failed to call Google API", e);
        }
    }
}
