package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.vptr.aimathtutor.service.ai.NonRetryableAiProviderException;
import de.vptr.aimathtutor.util.AppConstants;
import de.vptr.aimathtutor.util.RetryAnnotationVerifier;
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
        assertFalse(model.isBlank());
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
        RetryAnnotationVerifier.verifyRetryAnnotation(OpenAiService.class, "generateContent", String.class);
    }

    @Test
    @DisplayName("Should annotate generateJsonContent with @Retry using AppConstants values")
    void generateJsonContentShouldHaveRetryAnnotation() throws NoSuchMethodException {
        RetryAnnotationVerifier.verifyRetryAnnotation(OpenAiService.class, "generateJsonContent", String.class);
    }
}
