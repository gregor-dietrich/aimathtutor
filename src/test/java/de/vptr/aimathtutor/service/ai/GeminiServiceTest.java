package de.vptr.aimathtutor.service.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.util.RetryAnnotationVerifier;
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
        assertFalse(model.isBlank());
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
        RetryAnnotationVerifier.verifyRetryAnnotation(GeminiService.class, "generateContent", String.class);
    }
}
