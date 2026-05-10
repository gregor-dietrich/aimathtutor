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
class GoogleServiceTest {

    @Inject
    GoogleService googleService;

    @Test
    @DisplayName("Should expose configured Google model")
    void shouldReturnConfiguredModel() {
        final String model = this.googleService.getModel();

        assertNotNull(model);
        assertFalse(model.isBlank());
    }

    @Test
    @DisplayName("Should report stable configuration state")
    void shouldReportConfigurationState() {
        final boolean first = this.googleService.isConfigured();
        final boolean second = this.googleService.isConfigured();
        assertEquals(first, second, "isConfigured() must be deterministic within a single request");
    }

    @Test
    @DisplayName("Should annotate generateContent with @Retry using AppConstants values")
    void generateContentShouldHaveRetryAnnotation() throws NoSuchMethodException {
        RetryAnnotationVerifier.verifyRetryAnnotation(GoogleService.class, "generateContent", String.class);
    }

    @Test
    @DisplayName("getConfigPrefix returns the Google prefix")
    void testGetConfigPrefix() {
        assertEquals("google", this.googleService.getConfigPrefix());
    }

    @Test
    @DisplayName("getDefaultModel returns the default model")
    void testGetDefaultModel() {
        assertEquals("gemma-4-31b-it", this.googleService.getDefaultModel());
    }

    @Test
    @DisplayName("getProviderName returns Google")
    void testGetProviderName() {
        assertEquals("Google", this.googleService.getProviderName());
    }
}
