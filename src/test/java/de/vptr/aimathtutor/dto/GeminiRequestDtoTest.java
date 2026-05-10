package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NullAway")
class GeminiRequestDtoTest {

    @Test
    @DisplayName("Default constructor has null fields")
    void testDefaultConstructor() {
        final var dto = new GeminiRequestDto();
        assertNull(dto.contents);
        assertNull(dto.generationConfig);
        assertNull(dto.safetySettings);
    }

    @Test
    @DisplayName("Content default constructor")
    void testContentDefault() {
        final var content = new GeminiRequestDto.Content();
        assertNull(content.parts);
        assertNull(content.role);
    }

    @Test
    @DisplayName("Content text constructor")
    void testContentTextConstructor() {
        final var content = new GeminiRequestDto.Content("Hello");
        assertNotNull(content.parts);
        assertEquals(1, content.parts.size());
        assertEquals("Hello", content.parts.get(0).text);
        assertEquals("user", content.role);
    }

    @Test
    @DisplayName("Part default constructor")
    void testPartDefault() {
        final var part = new GeminiRequestDto.Part();
        assertNull(part.text);
    }

    @Test
    @DisplayName("Part text constructor")
    void testPartTextConstructor() {
        final var part = new GeminiRequestDto.Part("text");
        assertEquals("text", part.text);
    }

    @Test
    @DisplayName("GenerationConfig default constructor")
    void testGenerationConfigDefault() {
        final var config = new GeminiRequestDto.GenerationConfig();
        assertNull(config.temperature);
        assertNull(config.maxOutputTokens);
        assertNull(config.topP);
        assertNull(config.topK);
    }

    @Test
    @DisplayName("GenerationConfig parameterized constructor")
    void testGenerationConfigParameterized() {
        final var config = new GeminiRequestDto.GenerationConfig(0.7, 100);
        assertEquals(0.7, config.temperature);
        assertEquals(100, config.maxOutputTokens);
    }

    @Test
    @DisplayName("SafetySetting default constructor")
    void testSafetySettingDefault() {
        final var setting = new GeminiRequestDto.SafetySetting();
        assertNull(setting.category);
        assertNull(setting.threshold);
    }

    @Test
    @DisplayName("SafetySetting parameterized constructor")
    void testSafetySettingParameterized() {
        final var setting = new GeminiRequestDto.SafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_LOW_AND_ABOVE");
        assertEquals("HARM_CATEGORY_HARASSMENT", setting.category);
        assertEquals("BLOCK_LOW_AND_ABOVE", setting.threshold);
    }

    @Test
    @DisplayName("createTextRequest creates valid request")
    void testCreateTextRequest() {
        final var request = GeminiRequestDto.createTextRequest("prompt", 0.5, 200);
        assertNotNull(request.contents);
        assertEquals(1, request.contents.size());
        assertEquals("prompt", request.contents.get(0).parts.get(0).text);
        assertNotNull(request.generationConfig);
        assertEquals(0.5, request.generationConfig.temperature);
        assertEquals(200, request.generationConfig.maxOutputTokens);
        assertNotNull(request.safetySettings);
        assertEquals(4, request.safetySettings.size());
    }
}
