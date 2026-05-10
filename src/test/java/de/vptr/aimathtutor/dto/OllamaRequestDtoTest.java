package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NullAway")
class OllamaRequestDtoTest {

    @Test
    @DisplayName("Default constructor has null fields")
    void testDefaultConstructor() {
        final var dto = new OllamaRequestDto();
        assertNull(dto.model);
        assertNull(dto.prompt);
        assertNull(dto.stream);
        assertNull(dto.options);
    }

    @Test
    @DisplayName("Options default constructor")
    void testOptionsDefault() {
        final var options = new OllamaRequestDto.Options();
        assertNull(options.temperature);
        assertNull(options.numPredict);
        assertNull(options.topP);
        assertNull(options.topK);
    }

    @Test
    @DisplayName("Options parameterized constructor")
    void testOptionsParameterized() {
        final var options = new OllamaRequestDto.Options(0.7, 100);
        assertEquals(0.7, options.temperature);
        assertEquals(100, options.numPredict);
    }

    @Test
    @DisplayName("createGenerateRequest creates valid request")
    void testCreateGenerateRequest() {
        final var request = OllamaRequestDto.createGenerateRequest("Hello", "llama3", 0.5, 200);
        assertEquals("llama3", request.model);
        assertEquals("Hello", request.prompt);
        assertEquals(false, request.stream);
        assertNotNull(request.options);
        assertEquals(0.5, request.options.temperature);
        assertEquals(200, request.options.numPredict);
    }
}
