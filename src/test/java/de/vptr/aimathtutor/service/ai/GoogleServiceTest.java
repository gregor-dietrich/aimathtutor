package de.vptr.aimathtutor.service.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.vptr.aimathtutor.dto.GoogleResponseDto;
import de.vptr.aimathtutor.exception.ProviderException;
import de.vptr.aimathtutor.util.RetryAnnotationVerifier;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@SuppressWarnings("NullAway")
class GoogleServiceTest extends AbstractAiServiceTest {

    @Inject
    GoogleService googleService;

    @Inject
    ObjectMapper objectMapper;

    private HttpClient mockHttpClient;
    private HttpResponse<String> mockResponse;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws IOException, InterruptedException {
        this.mockHttpClient = mock(HttpClient.class);
        this.mockResponse = mock(HttpResponse.class);

        when(this.mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(this.mockResponse);

        this.googleService.setHttpClient(this.mockHttpClient);

        mockCommonConfig();
    }

    @Test
    @DisplayName("generateContent should return content on success")
    void testGenerateContentSuccess() throws IOException {
        when(this.mockResponse.statusCode()).thenReturn(200);

        final var googleResponse = new GoogleResponseDto();
        final var candidate = new GoogleResponseDto.Candidate();
        final var content = new GoogleResponseDto.Content();
        final var part = new GoogleResponseDto.Part();
        part.text = "Google response";
        content.parts = List.of(part);
        candidate.content = content;
        googleResponse.candidates = List.of(candidate);

        when(this.mockResponse.body()).thenReturn(this.objectMapper.writeValueAsString(googleResponse));

        final String result = this.googleService.generateContent("Test prompt");
        assertEquals("Google response", result);
    }

    @Test
    @DisplayName("generateContent should throw exception on HTTP error")
    void testGenerateContentHttpError() {
        when(this.mockResponse.statusCode()).thenReturn(500);
        when(this.mockResponse.body()).thenReturn("Internal Server Error");

        assertThrows(ProviderException.class, () -> this.googleService.generateContent("Test prompt"));
    }

    @Test
    @DisplayName("Should expose model name from config (default gemini-3.1-flash-lite)")
    void shouldReturnConfiguredModel() {
        final String model = this.googleService.getModel();
        assertNotNull(model);
        assertFalse(model.isBlank());
    }

    @Test
    @DisplayName("Should report a stable configuration state via isConfigured")
    void shouldReportConfigurationState() {
        final boolean configured = this.googleService.isConfigured();
        assertEquals(configured, this.googleService.isConfigured());
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
        assertEquals("gemini-3.1-flash-lite", this.googleService.getDefaultModel());
    }

    @Test
    @DisplayName("getProviderName returns Google")
    void testGetProviderName() {
        assertEquals("Google", this.googleService.getProviderName());
    }
}
