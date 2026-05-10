package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NullAway")
class UserGroupDtoTest {

    @Test
    @DisplayName("Default constructor has null fields")
    void testDefaultConstructor() {
        final var dto = new UserGroupDto();
        assertNull(dto.publicId);
        assertNull(dto.name);
    }
}
