package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NullAway")
class ProviderTestResultDtoTest {

    @Test
    @DisplayName("Default constructor has defaults")
    void testDefaultConstructor() {
        final var dto = new ProviderTestResultDto();
        assertFalse(dto.success);
        assertNull(dto.message);
    }

    @Test
    @DisplayName("Parameterized constructor sets fields")
    void testParameterizedConstructor() {
        final var dto = new ProviderTestResultDto(true, "Connection OK");
        assertTrue(dto.success);
        assertEquals("Connection OK", dto.message);
    }

    @Test
    @DisplayName("ok factory method")
    void testOk() {
        final var dto = ProviderTestResultDto.ok("All good");
        assertTrue(dto.success);
        assertEquals("All good", dto.message);
    }

    @Test
    @DisplayName("fail factory method")
    void testFail() {
        final var dto = ProviderTestResultDto.fail("Connection refused");
        assertFalse(dto.success);
        assertEquals("Connection refused", dto.message);
    }
}
