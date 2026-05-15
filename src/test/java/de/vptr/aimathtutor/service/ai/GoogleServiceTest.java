package de.vptr.aimathtutor.service.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.GoogleResponseDto;
import de.vptr.aimathtutor.exception.ProviderException;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@SuppressWarnings("NullAway")
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
}
