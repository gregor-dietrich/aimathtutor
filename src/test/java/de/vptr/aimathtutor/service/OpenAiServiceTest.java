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
class OpenAiServiceTest {

    @Inject
    OpenAiService openAiService;

    @Test
    @DisplayName("Should expose model name from config (default gpt-5-nano)")
    void shouldReturnConfiguredModel() {
        final String model = this.openAiService.getModel();

        assertNotNull(model);
        // Default is "gpt-5-nano"; config service may override.
        assertEquals(false, model.isBlank());
    }

    @Test
    @DisplayName("Should report a stable configuration state via isConfigured")
    void shouldReportConfigurationState() {
        // Just verify the call works; the result depends on env-var presence.
        final boolean configured = this.openAiService.isConfigured();
        // Either true or false is acceptable here; we only assert no exception.
        assertEquals(configured, this.openAiService.isConfigured(),
                "isConfigured should be deterministic across invocations");
    }

    @Test
    @DisplayName("Should annotate generateContent with @Retry using AppConstants values")
    void generateContentShouldHaveRetryAnnotation() throws NoSuchMethodException {
        final var method = OpenAiService.class.getMethod("generateContent", String.class);
        final Retry retry = method.getAnnotation(Retry.class);

        assertNotNull(retry, "generateContent should be annotated with @Retry");
        assertEquals(AppConstants.RETRY_MAX_RETRIES, retry.maxRetries());
        assertEquals(AppConstants.RETRY_DELAY_MS, retry.delay());
        assertEquals(AppConstants.RETRY_JITTER_MS, retry.jitter());
        assertEquals(1, retry.abortOn().length);
        assertSame(NonRetryableAiProviderException.class, retry.abortOn()[0],
                "Permanent failures must abort retry");
    }

    @Test
    @DisplayName("Should annotate generateJsonContent with @Retry using AppConstants values")
    void generateJsonContentShouldHaveRetryAnnotation() throws NoSuchMethodException {
        final var method = OpenAiService.class.getMethod("generateJsonContent", String.class);
        final Retry retry = method.getAnnotation(Retry.class);

        assertNotNull(retry);
        assertEquals(AppConstants.RETRY_MAX_RETRIES, retry.maxRetries());
        assertEquals(AppConstants.RETRY_DELAY_MS, retry.delay());
        assertEquals(1, retry.abortOn().length);
        assertSame(NonRetryableAiProviderException.class, retry.abortOn()[0]);
    }
}
