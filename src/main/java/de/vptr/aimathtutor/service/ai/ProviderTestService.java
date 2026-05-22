package de.vptr.aimathtutor.service.ai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

import org.jboss.logging.Logger;

import de.vptr.aimathtutor.dto.ProviderTestResultDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service for testing connectivity to AI providers. Performs lightweight health checks without consuming API quota.
 */
@ApplicationScoped
public class ProviderTestService {

    private static final Logger LOG = Logger.getLogger(ProviderTestService.class);
    private static final int TEST_TIMEOUT_SECONDS = 5;

    @Inject
    GoogleService googleService;

    @Inject
    OpenAiService openAiService;

    @Inject
    OllamaService ollamaService;

    @Inject
    AiConfigService aiConfigService;

    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(TEST_TIMEOUT_SECONDS)).build();

    void setHttpClient(final HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Tests connection to the mock provider (always succeeds).
     */
    public ProviderTestResultDto testMock() {
        return ProviderTestResultDto.ok("Mock provider is always available");
    }

    /**
     * Tests connection to Google. Verifies API key is configured and the endpoint is reachable.
     */
    public ProviderTestResultDto testGoogle() {
        if (!this.googleService.isConfigured()) {
            return ProviderTestResultDto.fail("Google API key not configured. Set the app.google.api.key property.");
        }

        final String baseUrl = this.aiConfigService.getConfigValue(AiConfigKeys.GOOGLE_API_BASE_URL,
                "https://generativelanguage.googleapis.com");
        try {
            this.aiConfigService.validateProviderApiUrl(baseUrl, AiConfigService.ProviderType.GOOGLE);
        } catch (final IllegalArgumentException e) {
            LOG.warnf("Google base URL rejected by SSRF guard: %s", e.getMessage());
            return ProviderTestResultDto.fail("Google base URL rejected: " + e.getMessage());
        }
        try {
            final HttpResponse<String> response = this.doGetRequest(baseUrl + "/v1beta/models");

            if (response.statusCode() == 401 || response.statusCode() == 403) {
                return ProviderTestResultDto.ok("Google endpoint is reachable (authentication required)");
            }
            if (response.statusCode() == 200) {
                return ProviderTestResultDto.ok("Google connection successful");
            }
            return ProviderTestResultDto.fail("Google returned unexpected status: " + response.statusCode());
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Google connection test interrupted", e);
            return ProviderTestResultDto.fail("Connection test interrupted");
        } catch (final IOException e) {
            LOG.warnf(e, "Google endpoint unreachable: %s", e.getMessage());
            return ProviderTestResultDto.fail("Cannot reach Google endpoint: " + e.getMessage());
        } catch (final IllegalArgumentException e) {
            LOG.warnf(e, "Invalid Google base URL: %s", baseUrl);
            return ProviderTestResultDto.fail("Invalid Google base URL: " + e.getMessage());
        }
    }

    /**
     * Tests connection to OpenAI. Verifies API key is configured and the endpoint is reachable.
     */
    public ProviderTestResultDto testOpenAi() {
        if (!this.openAiService.isConfigured()) {
            return ProviderTestResultDto.fail("OpenAI API key not configured. Set the app.openai.api.key property.");
        }

        final String baseUrl =
                this.aiConfigService.getConfigValue(AiConfigKeys.OPENAI_API_BASE_URL, "https://api.openai.com/v1");
        try {
            this.aiConfigService.validateProviderApiUrl(baseUrl, AiConfigService.ProviderType.OPENAI);
        } catch (final IllegalArgumentException e) {
            LOG.warnf("OpenAI base URL rejected by SSRF guard: %s", e.getMessage());
            return ProviderTestResultDto.fail("OpenAI base URL rejected: " + e.getMessage());
        }
        try {
            final HttpResponse<String> response = this.doGetRequest(baseUrl + "/models");

            if (response.statusCode() == 401 || response.statusCode() == 403) {
                return ProviderTestResultDto.ok("OpenAI endpoint is reachable (authentication required)");
            }
            if (response.statusCode() == 200) {
                return ProviderTestResultDto.ok("OpenAI connection successful");
            }
            return ProviderTestResultDto.fail("OpenAI returned unexpected status: " + response.statusCode());
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("OpenAI connection test interrupted", e);
            return ProviderTestResultDto.fail("Connection test interrupted");
        } catch (final IOException e) {
            LOG.warnf(e, "OpenAI endpoint unreachable: %s", e.getMessage());
            return ProviderTestResultDto.fail("Cannot reach OpenAI endpoint: " + e.getMessage());
        } catch (final IllegalArgumentException e) {
            LOG.warnf(e, "Invalid OpenAI base URL: %s", baseUrl);
            return ProviderTestResultDto.fail("Invalid OpenAI base URL: " + e.getMessage());
        }
    }

    /**
     * Tests connection to Ollama. Uses the Ollama /api/tags endpoint to verify the server is running.
     */
    public ProviderTestResultDto testOllama() {
        if (!this.ollamaService.isAvailable()) {
            return ProviderTestResultDto
                    .fail("Ollama server is not available. Check that Ollama is running and the URL is correct.");
        }
        return ProviderTestResultDto.ok("Ollama server is reachable");
    }

    private HttpResponse<String> doGetRequest(final String url) throws IOException, InterruptedException {
        final HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
                .timeout(Duration.ofSeconds(TEST_TIMEOUT_SECONDS)).GET().build();
        return this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Tests the currently configured AI provider.
     */
    public ProviderTestResultDto testCurrentProvider() {
        final String provider = this.aiConfigService.getConfigValue(AiConfigKeys.AI_TUTOR_PROVIDER, "mock");
        return switch (provider != null ? provider.toLowerCase(Locale.ROOT) : "mock") {
            case "google" -> this.testGoogle();
            case "openai" -> this.testOpenAi();
            case "ollama" -> this.testOllama();
            case "mock" -> this.testMock();
            default -> ProviderTestResultDto
                    .fail("Unknown AI provider: " + provider + ". Supported providers: google, openai, ollama");
        };
    }
}
