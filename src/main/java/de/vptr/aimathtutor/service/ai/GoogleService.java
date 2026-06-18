package de.vptr.aimathtutor.service.ai;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.logging.Logger;

import de.vptr.aimathtutor.dto.GoogleRequestDto;
import de.vptr.aimathtutor.dto.GoogleResponseDto;
import de.vptr.aimathtutor.exception.NonRetryableProviderException;
import de.vptr.aimathtutor.exception.ProviderException;
import de.vptr.aimathtutor.util.AppConstants;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Service for interacting with the Google AI API. Configuration is provided dynamically via {@link AiConfigService}.
 */
@ApplicationScoped
public class GoogleService extends AbstractProviderService {

    private static final Logger LOG = Logger.getLogger(GoogleService.class);
    private static final String DEFAULT_MODEL = "gemini-3.1-flash-lite";
    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com";

    @ConfigProperty(name = "app.google.api.key", defaultValue = "")
    @Nullable
    private String apiKey; // API key is always read from environment variable, never from database

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

        this.requireApiKey(this.apiKey, "app.google.api.key");

        // Load dynamic configuration
        final String model = this.aiConfigService.getConfigValue(AiConfigKeys.GOOGLE_MODEL, DEFAULT_MODEL);
        final String baseUrl = this.aiConfigService.getConfigValue(AiConfigKeys.GOOGLE_API_BASE_URL, DEFAULT_BASE_URL);
        final double temperature = this.aiConfigService.getClampedTemperature(AiConfigKeys.GOOGLE_TEMPERATURE, 0.7);
        final int maxTokens = this.aiConfigService.getClampedTokens(AiConfigKeys.GOOGLE_MAX_TOKENS, 2000);

        this.requireConfigured(model, "Google model");
        this.requireConfigured(baseUrl, "Google API URL");

        // The following checks satisfy NullAway; requireConfigured already threw if null
        if (model == null || baseUrl == null) {
            throw new IllegalStateException(
                    "GoogleService: missing configuration — " + (model == null ? "google.model" : "google.base-url"));
        }

        this.requireSafeProviderUrl(baseUrl, AiConfigService.ProviderType.GOOGLE);

        try {
            // Create request DTO
            final var requestDto = GoogleRequestDto.createTextRequest(prompt, temperature, maxTokens);

            // API URL (key moved to header to avoid appearing in logs/proxies)
            final String path = String.format("/v1beta/models/%s:generateContent", model);

            LOG.debugf("Calling Google API at: %s%s", baseUrl, path);

            try (Response response = this.getClient().target(baseUrl).path(path).request(MediaType.APPLICATION_JSON)
                    .header("x-goog-api-key", this.apiKey).post(Entity.json(requestDto))) {

                final int statusCode = response.getStatus();
                if (statusCode != 200) {
                    final String errorBody = response.readEntity(String.class);
                    LOG.errorf("Google API error (status %s): %s", statusCode, errorBody);
                    throw ProviderException.httpFailure(this.getProviderName(), statusCode, errorBody);
                }

                // Parse response
                final var googleResponse = response.readEntity(GoogleResponseDto.class);

                if (googleResponse.isBlocked()) {
                    LOG.warn("Google response was blocked by safety filters");
                    throw new NonRetryableProviderException(this.getProviderName(),
                            "Response blocked by safety filters");
                }

                if (googleResponse.isTruncated()) {
                    LOG.warnf("Google response was truncated due to token limit (finishReason=%s)",
                            googleResponse.getFinishReason());
                }

                final String content = this.requireNonEmptyContent(googleResponse.getTextContent());

                LOG.debugf("Successfully generated content from Google, length: %s", content.length());
                return content;
            }

        } catch (final ProviderException e) {
            throw e;
        } catch (final Exception e) {
            LOG.error("Error calling Google API", e);
            throw ProviderException.transportFailure(this.getProviderName(), "Failed to call Google API", e);
        }
    }
}
