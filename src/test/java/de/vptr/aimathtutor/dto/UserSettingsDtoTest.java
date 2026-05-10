package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NullAway")
class UserSettingsDtoTest {

    @Test
    @DisplayName("Default constructor has null fields")
    void testDefaultConstructor() {
        final var dto = new UserSettingsDto();
        assertNull(dto.currentPassword);
        assertNull(dto.newPassword);
        assertNull(dto.userAvatarEmoji);
        assertNull(dto.tutorAvatarEmoji);
    }

    @Test
    @DisplayName("Parameterized constructor sets avatar fields")
    void testParameterizedConstructor() {
        final var dto = new UserSettingsDto("😀", "🎓");
        assertEquals("😀", dto.userAvatarEmoji);
        assertEquals("🎓", dto.tutorAvatarEmoji);
    }
}
