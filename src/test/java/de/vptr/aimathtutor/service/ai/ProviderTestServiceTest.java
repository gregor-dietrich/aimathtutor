package de.vptr.aimathtutor.service.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.vptr.aimathtutor.dto.ProviderTestResultDto;
import de.vptr.aimathtutor.dto.SessionCredentials;
import de.vptr.aimathtutor.exception.PermissionDeniedException;
import de.vptr.aimathtutor.util.AppConstants;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.validation.ValidationException;

@QuarkusTest
class ProviderTestServiceTest {

    private static final SessionCredentials CREDENTIALS = new SessionCredentials("pub", "stamp", "key");

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

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("testGoogle returns success when response is 200")
    void testTestGoogle_success200() throws IOException, InterruptedException {
        this.setupGoogleMock(200);
        final ProviderTestResultDto result = this.aiProviderTestService.testGoogle(CREDENTIALS);
        assertTrue(result.success);
        assertTrue(result.message.contains("successful"));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("testGoogle returns success when response is 401 (auth required)")
    void testTestGoogle_reachable401() throws IOException, InterruptedException {
        this.setupGoogleMock(401);
        final ProviderTestResultDto result = this.aiProviderTestService.testGoogle(CREDENTIALS);
        assertTrue(result.success);
        assertTrue(result.message.contains("reachable"));
    }

    @SuppressWarnings({ "NullAway", "unchecked" })
    private void setupGoogleMock(final int statusCode) throws IOException, InterruptedException {
        when(this.mockGoogleService.isConfigured()).thenReturn(true);
        when(this.mockAiConfigService.getConfigValue(eq(AiConfigKeys.GOOGLE_API_BASE_URL), any()))
                .thenReturn("https://google.com");
        final HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(statusCode);
        when(this.mockHttpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);
    }

    @ParameterizedTest
    @ValueSource(strings = { "google", "openai" })
    @DisplayName("provider tests report an unreachable endpoint without the transport error's text")
    @SuppressWarnings({ "NullAway", "unchecked" })
    void testProviderTest_ioException(final String provider) throws IOException, InterruptedException {
        final var isGoogle = "google".equals(provider);
        when(isGoogle ? this.mockGoogleService.isConfigured() : this.mockOpenAiService.isConfigured()).thenReturn(true);
        when(this.mockAiConfigService.getConfigValue(any(), any())).thenReturn("https://example.com");
        when(this.mockHttpClient.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Connection refused"));

        final ProviderTestResultDto result = this.callProvider(provider, CREDENTIALS);
        assertFalse(result.success);
        assertTrue(result.message.contains("Cannot reach " + (isGoogle ? "Google" : "OpenAI") + " endpoint"),
                result.message);
        assertFalse(result.message.contains("Connection refused"), result.message);
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("testGoogle returns failure when not configured")
    void testTestGoogle_notConfigured() {
        when(this.mockGoogleService.isConfigured()).thenReturn(false);
        final ProviderTestResultDto result = this.aiProviderTestService.testGoogle(CREDENTIALS);
        assertNotNull(result);
        assertFalse(result.success);
        assertTrue(result.message.contains("not configured"));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("testGoogle returns failure when SSRF guard rejects URL")
    void testTestGoogle_ssrfRejected() {
        this.setupSsrfMock(AiConfigKeys.GOOGLE_API_BASE_URL, AiConfigService.ProviderType.GOOGLE, true);
        final ProviderTestResultDto result = this.aiProviderTestService.testGoogle(CREDENTIALS);
        assertFalse(result.success);
        assertTrue(result.message.contains("SSRF blocked"));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("testOpenAi returns failure when not configured")
    void testTestOpenAi_notConfigured() {
        when(this.mockOpenAiService.isConfigured()).thenReturn(false);
        final ProviderTestResultDto result = this.aiProviderTestService.testOpenAi(CREDENTIALS);
        assertNotNull(result);
        assertFalse(result.success);
        assertTrue(result.message.contains("not configured"));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("testOpenAi returns failure when SSRF guard rejects URL")
    void testTestOpenAi_ssrfRejected() {
        this.setupSsrfMock(AiConfigKeys.OPENAI_API_BASE_URL, AiConfigService.ProviderType.OPENAI, false);
        final ProviderTestResultDto result = this.aiProviderTestService.testOpenAi(CREDENTIALS);
        assertFalse(result.success);
        assertTrue(result.message.contains("SSRF blocked"));
    }

    @SuppressWarnings("NullAway")
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

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("testOllama returns failure when not available")
    void testTestOllama_notAvailable() {
        when(this.mockOllamaService.isAvailable()).thenReturn(false);
        final ProviderTestResultDto result = this.aiProviderTestService.testOllama(CREDENTIALS);
        assertNotNull(result);
        assertFalse(result.success);
        assertTrue(result.message.contains("not available"));
    }

    private ProviderTestResultDto callProvider(final String provider, final SessionCredentials credentials) {
        return switch (provider) {
            case "google" -> this.aiProviderTestService.testGoogle(credentials);
            case "openai" -> this.aiProviderTestService.testOpenAi(credentials);
            case "ollama" -> this.aiProviderTestService.testOllama(credentials);
            default -> throw new IllegalArgumentException("Unknown provider: " + provider);
        };
    }

    @SuppressWarnings("NullAway")
    private void verifyNothingTouched() {
        verifyNoInteractions(this.mockGoogleService, this.mockOpenAiService, this.mockOllamaService,
                this.mockHttpClient);
        verify(this.mockAiConfigService, never()).getConfigValue(any(), any());
        verify(this.mockAiConfigService, never()).validateProviderApiUrl(any(), any());
    }

    @SuppressWarnings("NullAway")
    @ParameterizedTest
    @ValueSource(strings = { "google", "openai", "ollama" })
    @DisplayName("provider tests refuse an ended session before any lookup or request")
    void testProviderTest_sessionEnded(final String provider) {
        final var ended = new ValidationException(AppConstants.SESSION_ENDED_MESSAGE);
        doThrow(ended).when(this.mockAiConfigService).requireConfigEditPermission(eq(CREDENTIALS));
        final var thrown = assertThrows(ValidationException.class, () -> this.callProvider(provider, CREDENTIALS));
        assertSame(ended, thrown);
        this.verifyNothingTouched();
    }

    @SuppressWarnings("NullAway")
    @ParameterizedTest
    @ValueSource(strings = { "google", "openai", "ollama" })
    @DisplayName("provider tests refuse a caller without the permission before any lookup or request")
    void testProviderTest_permissionDenied(final String provider) {
        doThrow(new PermissionDeniedException("denied")).when(this.mockAiConfigService)
                .requireConfigEditPermission(eq(CREDENTIALS));
        assertThrows(PermissionDeniedException.class, () -> this.callProvider(provider, CREDENTIALS));
        this.verifyNothingTouched();
    }
}
