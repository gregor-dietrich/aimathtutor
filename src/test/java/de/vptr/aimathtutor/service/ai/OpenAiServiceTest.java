package de.vptr.aimathtutor.service.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.OpenAiResponseDto;
import de.vptr.aimathtutor.exception.ProviderException;
import de.vptr.aimathtutor.util.RetryAnnotationVerifier;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@SuppressWarnings("NullAway")
class OpenAiServiceTest extends AbstractJaxRsAiServiceTest {

    @Inject
    OpenAiService openAiService;

    @Override
    protected void setupClient() {
        when(this.mockBuilder.header(anyString(), any())).thenReturn(this.mockBuilder);
        this.openAiService.setClient(this.mockClient);
    }

    @Test
    @DisplayName("generateContent should return content on success")
    void testGenerateContentSuccess() {
        when(this.mockResponse.getStatus()).thenReturn(200);

        final var openAiResponse = new OpenAiResponseDto();
        final var choice = new OpenAiResponseDto.Choice();
        final var message = new OpenAiResponseDto.Message();
        message.content = "Test response";
        choice.message = message;
        openAiResponse.choices = List.of(choice);

        when(this.mockResponse.readEntity(OpenAiResponseDto.class)).thenReturn(openAiResponse);

        final String result = this.openAiService.generateContent("Test prompt");
        assertEquals("Test response", result);
    }

    @Test
    @DisplayName("generateContent should throw exception on HTTP error")
    void testGenerateContentHttpError() {
        when(this.mockResponse.getStatus()).thenReturn(500);
        when(this.mockResponse.readEntity(String.class)).thenReturn("Internal Server Error");

        assertThrows(ProviderException.class, () -> this.openAiService.generateContent("Test prompt"));
    }

    @Test
    @DisplayName("generateJsonContent should work similarly")
    void testGenerateJsonContentSuccess() {
        when(this.mockResponse.getStatus()).thenReturn(200);

        final var openAiResponse = new OpenAiResponseDto();
        final var choice = new OpenAiResponseDto.Choice();
        final var message = new OpenAiResponseDto.Message();
        message.content = "{\"answer\": 42}";
        choice.message = message;
        openAiResponse.choices = List.of(choice);

        when(this.mockResponse.readEntity(OpenAiResponseDto.class)).thenReturn(openAiResponse);

        final String result = this.openAiService.generateJsonContent("Test prompt");
        assertEquals("{\"answer\": 42}", result);
    }

    @Test
    @DisplayName("Should expose model name from config (default gpt-5-nano)")
    void shouldReturnConfiguredModel() {
        final String model = this.openAiService.getModel();
        assertNotNull(model);
        assertFalse(model.isBlank());
    }

    @Test
    @DisplayName("Should report a stable configuration state via isConfigured")
    void shouldReportConfigurationState() {
        final boolean configured = this.openAiService.isConfigured();
        assertEquals(configured, this.openAiService.isConfigured());
    }

    @Test
    @DisplayName("Should annotate generateContent with @Retry using AppConstants values")
    void generateContentShouldHaveRetryAnnotation() throws NoSuchMethodException {
        RetryAnnotationVerifier.verifyRetryAnnotation(OpenAiService.class, "generateContent", String.class);
    }

    @Test
    @DisplayName("Should annotate generateJsonContent with @Retry using AppConstants values")
    void generateJsonContentShouldHaveRetryAnnotation() throws NoSuchMethodException {
        RetryAnnotationVerifier.verifyRetryAnnotation(OpenAiService.class, "generateJsonContent", String.class);
    }

    @Test
    @DisplayName("cleanup should complete without exception and leave getters working")
    void testCleanup() {
        this.openAiService.cleanup();
        this.openAiService.cleanup();
        assertNotNull(this.openAiService.getModel());
    }

    @Test
    @DisplayName("getConfigPrefix returns the OpenAI prefix")
    void testGetConfigPrefix() {
        assertEquals("openai", this.openAiService.getConfigPrefix());
    }

    @Test
    @DisplayName("getDefaultModel returns the default model")
    void testGetDefaultModel() {
        assertEquals("gpt-5-nano", this.openAiService.getDefaultModel());
    }

    @Test
    @DisplayName("getProviderName returns OpenAI")
    void testGetProviderName() {
        assertEquals("OpenAI", this.openAiService.getProviderName());
    }
}
