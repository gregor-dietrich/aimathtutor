package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NullAway")
class AiConfigDtoTest {

    @Test
    @DisplayName("Default constructor has null fields")
    void testDefaultConstructor() {
        final var dto = new AiConfigDto();
        assertNull(dto.publicId);
        assertNull(dto.configKey);
        assertNull(dto.configValue);
        assertNull(dto.configType);
        assertNull(dto.category);
        assertNull(dto.description);
        assertNull(dto.lastUpdatedAt);
        assertNull(dto.lastUpdatedBy);
    }

    @Test
    @DisplayName("Required-fields constructor sets core fields")
    void testRequiredFieldsConstructor() {
        final var dto =
                new AiConfigDto("model", "gpt-4", AiConfigDto.ConfigType.STRING, AiConfigDto.ConfigCategory.OPENAI);
        assertEquals("model", dto.configKey);
        assertEquals("gpt-4", dto.configValue);
        assertEquals(AiConfigDto.ConfigType.STRING, dto.configType);
        assertEquals(AiConfigDto.ConfigCategory.OPENAI, dto.category);
        assertNotNull(dto.lastUpdatedAt);
    }

    @Test
    @DisplayName("All-fields constructor sets all fields")
    void testAllFieldsConstructor() {
        final var now = LocalDateTime.now(ZoneId.systemDefault());
        final var dto = new AiConfigDto("pid", "temperature", "0.7", AiConfigDto.ConfigType.DOUBLE,
                AiConfigDto.ConfigCategory.GENERAL, "Controls randomness", now, "admin");
        assertEquals("pid", dto.publicId);
        assertEquals("temperature", dto.configKey);
        assertEquals("0.7", dto.configValue);
        assertEquals(AiConfigDto.ConfigType.DOUBLE, dto.configType);
        assertEquals(AiConfigDto.ConfigCategory.GENERAL, dto.category);
        assertEquals("Controls randomness", dto.description);
        assertEquals(now, dto.lastUpdatedAt);
        assertEquals("admin", dto.lastUpdatedBy);
    }

    @Test
    @DisplayName("ConfigType.fromString returns matching type")
    void testConfigTypeFromString() {
        assertEquals(AiConfigDto.ConfigType.STRING, AiConfigDto.ConfigType.fromString("STRING"));
        assertEquals(AiConfigDto.ConfigType.INTEGER, AiConfigDto.ConfigType.fromString("INTEGER"));
        assertEquals(AiConfigDto.ConfigType.DOUBLE, AiConfigDto.ConfigType.fromString("DOUBLE"));
        assertEquals(AiConfigDto.ConfigType.BOOLEAN, AiConfigDto.ConfigType.fromString("BOOLEAN"));
        assertEquals(AiConfigDto.ConfigType.TEXT, AiConfigDto.ConfigType.fromString("TEXT"));
    }

    @Test
    @DisplayName("ConfigType.fromString is case-insensitive")
    void testConfigTypeFromStringCaseInsensitive() {
        assertEquals(AiConfigDto.ConfigType.STRING, AiConfigDto.ConfigType.fromString("string"));
        assertEquals(AiConfigDto.ConfigType.INTEGER, AiConfigDto.ConfigType.fromString("Integer"));
    }

    @Test
    @DisplayName("ConfigType.fromString returns null for unknown value")
    void testConfigTypeFromStringUnknown() {
        assertNull(AiConfigDto.ConfigType.fromString("UNKNOWN"));
    }

    @Test
    @DisplayName("ConfigType.fromString returns null for null input")
    void testConfigTypeFromStringNull() {
        assertNull(AiConfigDto.ConfigType.fromString(null));
    }

    @Test
    @DisplayName("ConfigType getValue returns correct value")
    void testConfigTypeGetValue() {
        assertEquals("STRING", AiConfigDto.ConfigType.STRING.getValue());
        assertEquals("INTEGER", AiConfigDto.ConfigType.INTEGER.getValue());
        assertEquals("DOUBLE", AiConfigDto.ConfigType.DOUBLE.getValue());
        assertEquals("BOOLEAN", AiConfigDto.ConfigType.BOOLEAN.getValue());
        assertEquals("TEXT", AiConfigDto.ConfigType.TEXT.getValue());
    }

    @Test
    @DisplayName("ConfigType toString returns value")
    void testConfigTypeToString() {
        assertEquals("STRING", AiConfigDto.ConfigType.STRING.toString());
        assertEquals("BOOLEAN", AiConfigDto.ConfigType.BOOLEAN.toString());
    }

    @Test
    @DisplayName("ConfigCategory.fromString returns matching category")
    void testConfigCategoryFromString() {
        assertEquals(AiConfigDto.ConfigCategory.GENERAL, AiConfigDto.ConfigCategory.fromString("GENERAL"));
        assertEquals(AiConfigDto.ConfigCategory.GEMINI, AiConfigDto.ConfigCategory.fromString("GEMINI"));
        assertEquals(AiConfigDto.ConfigCategory.OPENAI, AiConfigDto.ConfigCategory.fromString("OPENAI"));
        assertEquals(AiConfigDto.ConfigCategory.OLLAMA, AiConfigDto.ConfigCategory.fromString("OLLAMA"));
        assertEquals(AiConfigDto.ConfigCategory.PROMPTS, AiConfigDto.ConfigCategory.fromString("PROMPTS"));
    }

    @Test
    @DisplayName("ConfigCategory.fromString is case-insensitive")
    void testConfigCategoryFromStringCaseInsensitive() {
        assertEquals(AiConfigDto.ConfigCategory.GENERAL, AiConfigDto.ConfigCategory.fromString("general"));
        assertEquals(AiConfigDto.ConfigCategory.GEMINI, AiConfigDto.ConfigCategory.fromString("Gemini"));
    }

    @Test
    @DisplayName("ConfigCategory.fromString returns null for unknown value")
    void testConfigCategoryFromStringUnknown() {
        assertNull(AiConfigDto.ConfigCategory.fromString("UNKNOWN"));
    }

    @Test
    @DisplayName("ConfigCategory.fromString returns null for null input")
    void testConfigCategoryFromStringNull() {
        assertNull(AiConfigDto.ConfigCategory.fromString(null));
    }

    @Test
    @DisplayName("ConfigCategory getValue returns correct value")
    void testConfigCategoryGetValue() {
        assertEquals("GENERAL", AiConfigDto.ConfigCategory.GENERAL.getValue());
        assertEquals("GEMINI", AiConfigDto.ConfigCategory.GEMINI.getValue());
        assertEquals("OPENAI", AiConfigDto.ConfigCategory.OPENAI.getValue());
        assertEquals("OLLAMA", AiConfigDto.ConfigCategory.OLLAMA.getValue());
        assertEquals("PROMPTS", AiConfigDto.ConfigCategory.PROMPTS.getValue());
    }

    @Test
    @DisplayName("ConfigCategory toString returns value")
    void testConfigCategoryToString() {
        assertEquals("GENERAL", AiConfigDto.ConfigCategory.GENERAL.toString());
        assertEquals("PROMPTS", AiConfigDto.ConfigCategory.PROMPTS.toString());
    }
}
