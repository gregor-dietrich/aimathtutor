package de.vptr.aimathtutor.service.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
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

    @Nullable
    @InjectMock
    GoogleService mockGoogleService;

    @Nullable
    @InjectMock
    OpenAiService mockOpenAiService;

    @Nullable
    @InjectMock
    OllamaService mockOllamaService;

    private HttpClient mockHttpClient;

    @BeforeEach
    void setUp() {
        this.mockHttpClient = mock(HttpClient.class);
        this.aiProviderTestService.setHttpClient(this.mockHttpClient);
    }

    @Test
    @DisplayName("testGoogle returns success when response is 200")
    void testTestGoogle_success200() throws IOException, InterruptedException {
        this.setupGoogleMock(200);
        final ProviderTestResultDto result = this.aiProviderTestService.testGoogle();
        assertTrue(result.success);
        assertTrue(result.message.contains("successful"));
    }

    @Test
    @DisplayName("testGoogle returns success when response is 401 (auth required)")
    void testTestGoogle_reachable401() throws IOException, InterruptedException {
        this.setupGoogleMock(401);
        final ProviderTestResultDto result = this.aiProviderTestService.testGoogle();
        assertTrue(result.success);
        assertTrue(result.message.contains("reachable"));
    }

    @SuppressWarnings("unchecked")
    private void setupGoogleMock(final int statusCode) throws IOException, InterruptedException {
        when(this.mockGoogleService.isConfigured()).thenReturn(true);
        when(this.mockAiConfigService.getConfigValue(eq(AiConfigKeys.GOOGLE_API_BASE_URL), any()))
                .thenReturn("https://google.com");
        final HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(statusCode);
        when(this.mockHttpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);
    }

    @Test
    @DisplayName("testGoogle returns failure when IOException occurs")
    @SuppressWarnings("unchecked")
    void testTestGoogle_ioException() throws IOException, InterruptedException {
        when(this.mockGoogleService.isConfigured()).thenReturn(true);
        when(this.mockAiConfigService.getConfigValue(eq(AiConfigKeys.GOOGLE_API_BASE_URL), any()))
                .thenReturn("https://google.com");
        when(this.mockHttpClient.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Connection refused"));

        final ProviderTestResultDto result = this.aiProviderTestService.testGoogle();
        assertFalse(result.success);
        assertTrue(result.message.contains("reach Google endpoint"));
    }

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
        when(this.mockAiConfigService.getConfigValue(eq(AiConfigKeys.AI_TUTOR_PROVIDER), any())).thenReturn("mock");
        final ProviderTestResultDto result = this.aiProviderTestService.testCurrentProvider();
        assertNotNull(result);
        assertTrue(result.success, "Seeded provider is mock — should succeed");
    }

    @Test
    @DisplayName("testGoogle returns failure when not configured")
    void testTestGoogle_notConfigured() {
        when(this.mockGoogleService.isConfigured()).thenReturn(false);
        final ProviderTestResultDto result = this.aiProviderTestService.testGoogle();
        assertNotNull(result);
        assertFalse(result.success);
        assertTrue(result.message.contains("not configured"));
    }

    @Test
    @DisplayName("testGoogle returns failure when SSRF guard rejects URL")
    void testTestGoogle_ssrfRejected() {
        this.setupSsrfMock(AiConfigKeys.GOOGLE_API_BASE_URL, AiConfigService.ProviderType.GOOGLE, true);
        final ProviderTestResultDto result = this.aiProviderTestService.testGoogle();
        assertFalse(result.success);
        assertTrue(result.message.contains("SSRF blocked"));
    }

    @Test
    @DisplayName("testOpenAi returns failure when not configured")
    void testTestOpenAi_notConfigured() {
        when(this.mockOpenAiService.isConfigured()).thenReturn(false);
        final ProviderTestResultDto result = this.aiProviderTestService.testOpenAi();
        assertNotNull(result);
        assertFalse(result.success);
        assertTrue(result.message.contains("not configured"));
    }

    @Test
    @DisplayName("testOpenAi returns failure when SSRF guard rejects URL")
    void testTestOpenAi_ssrfRejected() {
        this.setupSsrfMock(AiConfigKeys.OPENAI_API_BASE_URL, AiConfigService.ProviderType.OPENAI, false);
        final ProviderTestResultDto result = this.aiProviderTestService.testOpenAi();
        assertFalse(result.success);
        assertTrue(result.message.contains("SSRF blocked"));
    }

    private void setupSsrfMock(final String configKey, final AiConfigService.ProviderType type,
            final boolean isGoogle) {
        if (isGoogle) {
            when(this.mockGoogleService.isConfigured()).thenReturn(true);
        } else {
            when(this.mockOpenAiService.isConfigured()).thenReturn(true);
        }
        when(this.mockAiConfigService.getConfigValue(eq(configKey), any())).thenReturn("http://malicious.com");
        doThrow(new IllegalArgumentException("SSRF blocked")).when(this.mockAiConfigService)
                .validateProviderApiUrl(eq("http://malicious.com"), eq(type));
    }

    @Test
    @DisplayName("testOllama returns failure when not available")
    void testTestOllama_notAvailable() {
        when(this.mockOllamaService.isAvailable()).thenReturn(false);
        final ProviderTestResultDto result = this.aiProviderTestService.testOllama();
        assertNotNull(result);
        assertFalse(result.success);
        assertTrue(result.message.contains("not available"));
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

    @Test
    @DisplayName("testCurrentProvider delegates to testGoogle when provider is google")
    void testTestCurrentProvider_google() {
        when(this.mockAiConfigService.getConfigValue(eq(AiConfigKeys.AI_TUTOR_PROVIDER), any())).thenReturn("google");
        final ProviderTestResultDto result = this.aiProviderTestService.testCurrentProvider();
        assertNotNull(result);
        assertNotNull(result.message);
        assertFalse(result.message.isBlank());
    }

    @Test
    @DisplayName("testCurrentProvider delegates to testOpenAi when provider is openai")
    void testTestCurrentProvider_openai() {
        when(this.mockAiConfigService.getConfigValue(eq(AiConfigKeys.AI_TUTOR_PROVIDER), any())).thenReturn("openai");
        final ProviderTestResultDto result = this.aiProviderTestService.testCurrentProvider();
        assertNotNull(result);
        assertNotNull(result.message);
        assertFalse(result.message.isBlank());
    }

    @Test
    @DisplayName("testCurrentProvider delegates to testOllama when provider is ollama")
    void testTestCurrentProvider_ollama() {
        when(this.mockAiConfigService.getConfigValue(eq(AiConfigKeys.AI_TUTOR_PROVIDER), any())).thenReturn("ollama");
        final ProviderTestResultDto result = this.aiProviderTestService.testCurrentProvider();
        assertNotNull(result);
        assertNotNull(result.message);
        assertFalse(result.message.isBlank());
    }
}
