package de.vptr.aimathtutor.service.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.ProviderTestResultDto;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

@QuarkusTest
@SuppressWarnings("NullAway")
class ProviderTestServiceTest {

    @Inject
    ProviderTestService aiProviderTestService;

    @Nullable
    @InjectMock
    AiConfigService mockAiConfigService;

    @Test
    @DisplayName("testMock always returns success")
    void testTestMock() {
        final ProviderTestResultDto result = this.aiProviderTestService.testMock();
        assertNotNull(result);
        assertTrue(result.success);
        assertNotNull(result.message);
        assertFalse(result.message.isBlank());
    }

    @Test
    @DisplayName("testCurrentProvider returns success when provider is seeded as mock")
    void testTestCurrentProvider_mock() {
        final ProviderTestResultDto result = this.aiProviderTestService.testCurrentProvider();
        assertNotNull(result);
        assertTrue(result.success, "Seeded provider is mock — should succeed");
    }

    @Test
    @DisplayName("testGoogle returns a non-null result with a non-blank message")
    void testTestGoogle_returnsResult() {
        // GOOGLE_API_KEY may or may not be set — either success or failure is valid.
        final ProviderTestResultDto result = this.aiProviderTestService.testGoogle();
        assertNotNull(result);
        assertNotNull(result.message);
        assertFalse(result.message.isBlank());
    }

    @Test
    @DisplayName("testOpenAi returns a non-null result with a non-blank message")
    void testTestOpenAi_returnsResult() {
        // Without OPENAI_API_KEY: either fails immediately (not configured) or endpoint
        // is reachable (401 → ok). Either way the result is non-null with a message.
        final ProviderTestResultDto result = this.aiProviderTestService.testOpenAi();
        assertNotNull(result);
        assertNotNull(result.message);
        assertFalse(result.message.isBlank());
    }

    @Test
    @DisplayName("testOllama returns a non-null result with a non-blank message")
    void testTestOllama_returnsResult() {
        // Ollama may or may not be available — either success or failure is valid.
        final ProviderTestResultDto result = this.aiProviderTestService.testOllama();
        assertNotNull(result);
        assertNotNull(result.message);
        assertFalse(result.message.isBlank());
    }

    @Test
    @DisplayName("testCurrentProvider returns failure for unknown provider name")
    void testTestCurrentProvider_unknownProvider() {
        when(this.mockAiConfigService.getConfigValue(eq(AiConfigKeys.AI_TUTOR_PROVIDER), any()))
                .thenReturn("unknown_provider_xyz");
        final ProviderTestResultDto result = this.aiProviderTestService.testCurrentProvider();
        assertNotNull(result);
        assertFalse(result.success, "Unknown provider must produce a failure result");
        assertNotNull(result.message);
        assertTrue(result.message.contains("Unknown AI provider"),
                "Failure message must mention the unknown provider, got: " + result.message);
    }
}
