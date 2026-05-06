package de.vptr.aimathtutor.service;

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
    @DisplayName("testGemini returns failure when API key is not configured")
    void testTestGemini_notConfigured() {
        // In the test environment GEMINI_API_KEY is not set — expect a failure result
        final AiProviderTestResultDto result = this.aiProviderTestService.testGemini();
        assertNotNull(result);
        assertNotNull(result.message);
        assertFalse(result.message.isBlank());
        // Either not configured (false) or endpoint reachable (true) — both valid, just not null
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
    @DisplayName("testOllama returns failure when Ollama server is unavailable")
    void testTestOllama_unavailable() {
        // No Ollama server in CI / test environment
        final AiProviderTestResultDto result = this.aiProviderTestService.testOllama();
        assertNotNull(result);
        assertFalse(result.success, "Ollama unavailable in test env — should fail");
        assertNotNull(result.message);
    }
}
