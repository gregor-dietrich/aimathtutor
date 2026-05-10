package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NullAway")
class AiConfigUpdateDtoTest {

    @Test
    @DisplayName("Default constructor has null fields")
    void testDefaultConstructor() {
        final var dto = new AiConfigUpdateDto();
        assertNull(dto.configKey);
        assertNull(dto.configValue);
    }

    @Test
    @DisplayName("Parameterized constructor sets fields")
    void testParameterizedConstructor() {
        final var dto = new AiConfigUpdateDto("temperature", "0.8");
        assertEquals("temperature", dto.configKey);
        assertEquals("0.8", dto.configValue);
    }

    @Test
    @DisplayName("Parameterized constructor accepts null values")
    void testParameterizedConstructorNulls() {
        final var dto = new AiConfigUpdateDto(null, null);
        assertNull(dto.configKey);
        assertNull(dto.configValue);
    }
}
