package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NullAway")
class UserDtoTest {

    @Test
    @DisplayName("Default constructor has null fields")
    void testDefaultConstructor() {
        final var dto = new UserDto();
        assertNull(dto.publicId);
        assertNull(dto.username);
        assertNull(dto.password);
        assertNull(dto.email);
        assertNull(dto.rankPublicId);
        assertNull(dto.banned);
        assertNull(dto.activated);
        assertNull(dto.activationKey);
    }

    @Test
    @DisplayName("Parameterized constructor sets all fields")
    void testParameterizedConstructor() {
        final var dto = new UserDto("alice", "pass123", "alice@example.com", "rp1", false, true, "key123");
        assertEquals("alice", dto.username);
        assertEquals("pass123", dto.password);
        assertEquals("alice@example.com", dto.email);
        assertEquals("rp1", dto.rankPublicId);
        assertEquals(false, dto.banned);
        assertEquals(true, dto.activated);
        assertEquals("key123", dto.activationKey);
    }
}
