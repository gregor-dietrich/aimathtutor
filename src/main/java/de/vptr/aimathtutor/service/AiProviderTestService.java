package de.vptr.aimathtutor.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.vptr.aimathtutor.dto.AiProviderTestResultDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service for testing connectivity to AI providers.
 * Performs lightweight health checks without consuming API quota.
 */
@ApplicationScoped
public class AiProviderTestService {

    private static final Logger LOG = LoggerFactory.getLogger(AiProviderTestService.class);
    private static final int TEST_TIMEOUT_SECONDS = 5;

    @Inject
    GeminiService geminiService;

    @Inject
    OpenAiService openAiService;

    @Inject
    OllamaService ollamaService;

    @Inject
    AiConfigService aiConfigService;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(TEST_TIMEOUT_SECONDS))
            .build();

    /**
     * Tests connection to the mock provider (always succeeds).
     */
    public AiProviderTestResultDto testMock() {
        return AiProviderTestResultDto.ok("Mock provider is always available");
    }

    /**
     * Tests connection to Gemini.
     * Verifies API key is configured and the endpoint is reachable.
     */
    public AiProviderTestResultDto testGemini() {
        if (!this.geminiService.isConfigured()) {
            return AiProviderTestResultDto.fail(
                    "Gemini API key not configured. Set the GEMINI_API_KEY environment variable.");
        }

        final String baseUrl = this.aiConfigService.getConfigValue("gemini.api.base-url",
                "https://generativelanguage.googleapis.com");
        try {
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1beta/models"))
                    .timeout(Duration.ofSeconds(TEST_TIMEOUT_SECONDS))
                    .GET()
                    .build();

            final HttpResponse<String> response = this.httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401 || response.statusCode() == 403) {
                return AiProviderTestResultDto.ok("Gemini endpoint is reachable (authentication required)");
            }
            if (response.statusCode() == 200) {
                return AiProviderTestResultDto.ok("Gemini connection successful");
            }
            return AiProviderTestResultDto.fail(
                    "Gemini returned unexpected status: " + response.statusCode());
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Gemini connection test interrupted", e);
            return AiProviderTestResultDto.fail("Connection test interrupted");
        } catch (final IOException e) {
            LOG.warn("Gemini endpoint unreachable: {}", e.getMessage());
            return AiProviderTestResultDto.fail("Cannot reach Gemini endpoint: " + e.getMessage());
        }
    }

    /**
     * Tests connection to OpenAI.
     * Verifies API key is configured and the endpoint is reachable.
     */
    public AiProviderTestResultDto testOpenAi() {
        if (!this.openAiService.isConfigured()) {
            return AiProviderTestResultDto.fail(
                    "OpenAI API key not configured. Set the OPENAI_API_KEY environment variable.");
        }

        final String baseUrl = this.aiConfigService.getConfigValue("openai.api.base-url",
                "https://api.openai.com/v1");
        try {
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/models"))
                    .timeout(Duration.ofSeconds(TEST_TIMEOUT_SECONDS))
                    .GET()
                    .build();

            final HttpResponse<String> response = this.httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401) {
                return AiProviderTestResultDto.ok("OpenAI endpoint is reachable (authentication required)");
            }
            if (response.statusCode() == 200) {
                return AiProviderTestResultDto.ok("OpenAI connection successful");
            }
            return AiProviderTestResultDto.fail(
                    "OpenAI returned unexpected status: " + response.statusCode());
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("OpenAI connection test interrupted", e);
            return AiProviderTestResultDto.fail("Connection test interrupted");
        } catch (final IOException e) {
            LOG.warn("OpenAI endpoint unreachable: {}", e.getMessage());
            return AiProviderTestResultDto.fail("Cannot reach OpenAI endpoint: " + e.getMessage());
        }
    }

    /**
     * Tests connection to Ollama.
     * Uses the Ollama /api/tags endpoint to verify the server is running.
     */
    public AiProviderTestResultDto testOllama() {
        if (!this.ollamaService.isAvailable()) {
            return AiProviderTestResultDto.fail(
                    "Ollama server is not available. Check that Ollama is running and the URL is correct.");
        }
        return AiProviderTestResultDto.ok("Ollama server is reachable");
    }

    /**
     * Tests the currently configured AI provider.
     */
    public AiProviderTestResultDto testCurrentProvider() {
        final String provider = this.aiConfigService.getConfigValue("ai.tutor.provider", "mock");
        return switch (provider != null ? provider.toLowerCase() : "mock") {
            case "gemini" -> this.testGemini();
            case "openai" -> this.testOpenAi();
            case "ollama" -> this.testOllama();
            default -> this.testMock();
        };
    }
}
