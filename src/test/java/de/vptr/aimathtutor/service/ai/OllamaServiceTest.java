package de.vptr.aimathtutor.service.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.util.RetryAnnotationVerifier;
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
        assertFalse(url.isBlank());
    }

    @Test
    @DisplayName("Should expose default model name from config")
    void shouldReturnConfiguredModel() {
        final String model = this.ollamaService.getModel();

        assertNotNull(model);
        assertFalse(model.isBlank());
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
        RetryAnnotationVerifier.verifyRetryAnnotation(OllamaService.class, "generateContent", String.class);
    }
}
