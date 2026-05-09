package de.vptr.aimathtutor.service.ai.provider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.AiProviderTestResultDto;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class AiProviderTestServiceTest {

    @Inject
    AiProviderTestService aiProviderTestService;

    @Test
    @DisplayName("testMock always returns success")
    void testTestMock() {
        final AiProviderTestResultDto result = this.aiProviderTestService.testMock();
        assertNotNull(result);
        assertTrue(result.success);
        assertNotNull(result.message);
        assertFalse(result.message.isBlank());
    }

    @Test
    @DisplayName("testCurrentProvider returns success when provider is seeded as mock")
    void testTestCurrentProvider_mock() {
        final AiProviderTestResultDto result = this.aiProviderTestService.testCurrentProvider();
        assertNotNull(result);
        assertTrue(result.success, "Seeded provider is mock — should succeed");
    }

    @Test
    @DisplayName("testGemini returns a non-null result with a non-blank message")
    void testTestGemini_returnsResult() {
        // GEMINI_API_KEY may or may not be set — either success or failure is valid.
        final AiProviderTestResultDto result = this.aiProviderTestService.testGemini();
        assertNotNull(result);
        assertNotNull(result.message);
        assertFalse(result.message.isBlank());
    }

    @Test
    @DisplayName("testOpenAi returns a non-null result with a non-blank message")
    void testTestOpenAi_returnsResult() {
        // Without OPENAI_API_KEY: either fails immediately (not configured) or endpoint
        // is reachable (401 → ok). Either way the result is non-null with a message.
        final AiProviderTestResultDto result = this.aiProviderTestService.testOpenAi();
        assertNotNull(result);
        assertNotNull(result.message);
        assertFalse(result.message.isBlank());
    }

    @Test
    @DisplayName("testOllama returns a non-null result with a non-blank message")
    void testTestOllama_returnsResult() {
        // Ollama may or may not be available — either success or failure is valid.
        final AiProviderTestResultDto result = this.aiProviderTestService.testOllama();
        assertNotNull(result);
        assertNotNull(result.message);
        assertFalse(result.message.isBlank());
    }
}
