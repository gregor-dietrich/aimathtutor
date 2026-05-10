package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NullAway")
class OllamaTagsResponseDtoTest {

    @Test
    @DisplayName("Default constructor has null models")
    void testDefaultConstructor() {
        final var dto = new OllamaTagsResponseDto();
        assertNull(dto.models);
    }

    @Test
    @DisplayName("ModelInfo default constructor has null fields")
    void testModelInfoDefault() {
        final var info = new OllamaTagsResponseDto.ModelInfo();
        assertNull(info.name);
        assertNull(info.model);
    }
}
