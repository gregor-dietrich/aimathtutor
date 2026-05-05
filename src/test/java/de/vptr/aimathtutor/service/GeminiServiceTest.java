package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
class GeminiServiceTest {

    @Inject
    GeminiService geminiService;

    @Test
    @DisplayName("Should expose configured Gemini model")
    void shouldReturnConfiguredModel() {
        final String model = this.geminiService.getModel();

        assertNotNull(model);
        assertEquals(false, model.isBlank());
    }

    @Test
    @DisplayName("Should report stable configuration state")
    void shouldReportConfigurationState() {
        final boolean configured = this.geminiService.isConfigured();
        assertEquals(configured, this.geminiService.isConfigured());
    }

    @Test
    @DisplayName("Should annotate generateContent with @Retry using AppConstants values")
    void generateContentShouldHaveRetryAnnotation() throws NoSuchMethodException {
        final var method = GeminiService.class.getMethod("generateContent", String.class);
        final Retry retry = method.getAnnotation(Retry.class);

        assertNotNull(retry, "generateContent should be annotated with @Retry");
        assertEquals(AppConstants.RETRY_MAX_RETRIES, retry.maxRetries());
        assertEquals(AppConstants.RETRY_DELAY_MS, retry.delay());
        assertEquals(AppConstants.RETRY_JITTER_MS, retry.jitter());
        assertEquals(1, retry.abortOn().length);
        assertSame(NonRetryableAiProviderException.class, retry.abortOn()[0],
                "Permanent failures must abort retry");
    }
}
