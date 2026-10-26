package de.vptr.aimathtutor.service.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.vptr.aimathtutor.dto.GoogleResponseDto;
import de.vptr.aimathtutor.exception.ProviderException;
import de.vptr.aimathtutor.util.RetryAnnotationVerifier;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class GoogleServiceTest extends AbstractJaxRsAiServiceTest {

    @Inject
    GoogleService googleService;

    @Override
    protected void setupClient() {
        this.googleService.setClient(this.mockClient);
    }

    @Test
    @DisplayName("generateContent should return content on success")
    void testGenerateContentSuccess() {
        when(this.mockResponse.getStatus()).thenReturn(200);

        final var googleResponse = new GoogleResponseDto();
        final var candidate = new GoogleResponseDto.Candidate();
        final var content = new GoogleResponseDto.Content();
        final var part = new GoogleResponseDto.Part();
        part.text = "Google response";
        content.parts = List.of(part);
        candidate.content = content;
        googleResponse.candidates = List.of(candidate);

        when(this.mockResponse.readEntity(GoogleResponseDto.class)).thenReturn(googleResponse);

        final String result = this.googleService.generateContent("Test prompt");
        assertEquals("Google response", result);
    }

    @Test
    @DisplayName("generateContent should throw exception on HTTP error")
    void testGenerateContentHttpError() {
        when(this.mockResponse.getStatus()).thenReturn(500);
        when(this.mockResponse.readEntity(String.class)).thenReturn("Internal Server Error");

        assertThrowsExactly(ProviderException.class, () -> this.googleService.generateContent("Test prompt"));
    }

    @ParameterizedTest
    @ValueSource(ints = { 400, 401, 403, 404 })
    @DisplayName("generateContent should preserve permanent HTTP failures")
    void testGenerateContentPermanentHttpError(final int status) {
        this.assertPermanentHttpFailure(status, () -> this.googleService.generateContent("Test prompt"));
    }

    @Test
    @DisplayName("Should abort retries for permanent Google failures")
    void generateContentShouldHaveRetryAnnotation() throws NoSuchMethodException {
        RetryAnnotationVerifier.verifyRetryAnnotation(GoogleService.class, "generateContent", String.class);
    }

    @Test
    @DisplayName("Should expose model name from config (default gemini-3.1-flash-lite)")
    void shouldReturnConfiguredModel() {
        assertEquals(AiConfigService.defaultValue(AiConfigKeys.GOOGLE_MODEL), this.googleService.getModel());
    }

    @Test
    @DisplayName("Should report a stable configuration state via isConfigured")
    void shouldReportConfigurationState() {
        final boolean configured = this.googleService.isConfigured();
        assertEquals(configured, this.googleService.isConfigured());
    }

    @Test
    @DisplayName("generateContent should not call the client when the URL is rejected")
    void testGenerateContentRejectedUrl() {
        this.assertRejectedUrlStopsRequest(AiConfigService.ProviderType.GOOGLE,
                () -> this.googleService.generateContent("Test prompt"));
    }
}
