package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.service.ai.NonRetryableAiProviderException;
import de.vptr.aimathtutor.util.AppConstants;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class OllamaServiceTest {

    @Inject
    OllamaService ollamaService;

    @Test
    @DisplayName("Should expose configured API URL")
    void shouldExposeConfiguredApiUrl() {
        final String url = this.ollamaService.getApiUrl();

        assertNotNull(url);
        assertEquals(false, url.isBlank());
    }

    @Test
    @DisplayName("Should expose default model name from config")
    void shouldReturnConfiguredModel() {
        final String model = this.ollamaService.getModel();

        assertNotNull(model);
        assertEquals(false, model.isBlank());
    }

    @Test
    @DisplayName("Should report Ollama as unavailable in test environment")
    void shouldReportUnavailableWhenServerNotReachable() {
        // Test profile sets connect/read timeouts to 1s and devservices does not
        // start an Ollama server, so isAvailable should return false promptly.
        final boolean available = this.ollamaService.isAvailable();
        assertFalse(available, "Ollama server should not be reachable in test env");
    }

    @Test
    @DisplayName("Should report unknown model as not installed when server unreachable")
    void shouldReportModelNotInstalledWhenServerUnreachable() {
        assertFalse(this.ollamaService.isModelInstalled("nonexistent-model"));
    }

    @Test
    @DisplayName("Should report stable isConfigured state")
    void shouldReportConfiguredState() {
        // isConfigured() delegates to isAvailable() — which is false in test env.
        assertFalse(this.ollamaService.isConfigured());
    }

    @Test
    @DisplayName("Should annotate generateContent with @Retry using AppConstants values")
    void generateContentShouldHaveRetryAnnotation() throws NoSuchMethodException {
        final var method = OllamaService.class.getMethod("generateContent", String.class);
        final Retry retry = method.getAnnotation(Retry.class);

        assertNotNull(retry, "generateContent should be annotated with @Retry");
        assertEquals(AppConstants.RETRY_MAX_RETRIES, retry.maxRetries());
        assertEquals(AppConstants.RETRY_DELAY_MS, retry.delay());
        assertEquals(AppConstants.RETRY_JITTER_MS, retry.jitter());
        assertEquals(1, retry.abortOn().length);
        assertSame(NonRetryableAiProviderException.class, retry.abortOn()[0]);
    }
}
