package de.vptr.aimathtutor.service.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.OllamaResponseDto;
import de.vptr.aimathtutor.dto.OllamaTagsResponseDto;
import de.vptr.aimathtutor.exception.ProviderException;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@SuppressWarnings("NullAway")
class OllamaServiceTest extends AbstractJaxRsAiServiceTest {

    @Inject
    OllamaService ollamaService;

    @Override
    protected void setupClient() {
        when(this.mockBuilder.get()).thenReturn(this.mockResponse);
        this.ollamaService.setClient(this.mockClient);
    }

    @Test
    @DisplayName("generateContent should return content on success")
    void testGenerateContentSuccess() {
        when(this.mockResponse.getStatus()).thenReturn(200);

        final var ollamaResponse = new OllamaResponseDto();
        ollamaResponse.response = "Ollama response";
        ollamaResponse.done = true;

        when(this.mockResponse.readEntity(OllamaResponseDto.class)).thenReturn(ollamaResponse);

        final String result = this.ollamaService.generateContent("Test prompt");
        assertEquals("Ollama response", result);
    }

    @Test
    @DisplayName("generateContent should throw exception on HTTP error")
    void testGenerateContentHttpError() {
        when(this.mockResponse.getStatus()).thenReturn(500);
        when(this.mockResponse.readEntity(String.class)).thenReturn("Internal Server Error");

        assertThrows(ProviderException.class, () -> this.ollamaService.generateContent("Test prompt"));
    }

    @Test
    @DisplayName("isAvailable should return true when server responds with 200")
    void testIsAvailableSuccess() {
        when(this.mockResponse.getStatus()).thenReturn(200);
        assertTrue(this.ollamaService.isAvailable());
    }

    @Test
    @DisplayName("isModelInstalled should check tags endpoint")
    void testIsModelInstalled() {
        when(this.mockResponse.getStatus()).thenReturn(200);

        final var tagsResponse = new OllamaTagsResponseDto();
        final var model = new OllamaTagsResponseDto.ModelInfo();
        model.name = "llama3.2:3b";
        tagsResponse.models = List.of(model);

        when(this.mockResponse.readEntity(OllamaTagsResponseDto.class)).thenReturn(tagsResponse);

        assertTrue(this.ollamaService.isModelInstalled("llama3.2:3b"));
        assertFalse(this.ollamaService.isModelInstalled("non-existent"));
    }

    @Test
    @DisplayName("Should expose model name from config (default llama3.2:3b)")
    void shouldReturnConfiguredModel() {
        final String model = this.ollamaService.getModel();
        assertNotNull(model);
        assertEquals("llama3.2:3b", model);
    }

    @Test
    @DisplayName("Should report a configuration state via isConfigured")
    void shouldReportConfigurationState() {
        // isConfigured calls isAvailable, which uses mockClient
        when(this.mockResponse.getStatus()).thenReturn(200);
        assertTrue(this.ollamaService.isConfigured());
    }

    @Test
    @DisplayName("cleanup should close client")
    void testCleanup() {
        this.ollamaService.cleanup();
        verify(this.mockClient).close();
        assertNotNull(this.ollamaService.getModel());
    }

    @Test
    @DisplayName("getConfigPrefix returns the Ollama prefix")
    void testGetConfigPrefix() {
        assertEquals("ollama", this.ollamaService.getConfigPrefix());
    }

    @Test
    @DisplayName("getProviderName returns Ollama")
    void testGetProviderName() {
        assertEquals("Ollama", this.ollamaService.getProviderName());
    }
}
