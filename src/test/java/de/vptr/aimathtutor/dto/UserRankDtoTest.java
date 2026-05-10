package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NullAway")
class UserRankDtoTest {

    @Test
    @DisplayName("Default constructor has null name")
    void testDefaultConstructor() {
        final var dto = new UserRankDto();
        assertNull(dto.name);
    }

    @Test
    @DisplayName("Parameterized constructor sets name")
    void testParameterizedConstructor() {
        final var dto = new UserRankDto("Teacher");
        assertEquals("Teacher", dto.name);
    }

    @Test
    @DisplayName("Inherits permission fields from UserRankPermissions")
    void testInheritedFields() {
        final var dto = new UserRankDto("Admin");
        assertNull(dto.publicId);
        assertNull(dto.adminView);
        assertNull(dto.exerciseAdd);
        assertNull(dto.exerciseEdit);
        assertNull(dto.exerciseDelete);
        assertNull(dto.lessonAdd);
        assertNull(dto.userAdd);
    }
}
