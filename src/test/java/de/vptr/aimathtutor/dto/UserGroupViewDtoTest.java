package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.UserGroupEntity;

@SuppressWarnings("NullAway")
class UserGroupViewDtoTest {

    @Test
    @DisplayName("Default constructor has null fields")
    void testDefaultConstructor() {
        final var dto = new UserGroupViewDto();
        assertNull(dto.publicId);
        assertNull(dto.name);
        assertNull(dto.userCount);
    }

    @Test
    @DisplayName("Entity constructor maps fields")
    void testEntityConstructor() {
        final var entity = new UserGroupEntity();
        entity.publicId = "gp1";
        entity.name = "Math Class";

        final var dto = new UserGroupViewDto(entity);
        assertEquals("gp1", dto.publicId);
        assertEquals("Math Class", dto.name);
    }

    @Test
    @DisplayName("Entity constructor handles null entity")
    void testEntityConstructorNull() {
        final var dto = new UserGroupViewDto((UserGroupEntity) null);
        assertNull(dto.publicId);
    }

    @Test
    @DisplayName("toUserGroupDto converts")
    void testToUserGroupDto() {
        final var viewDto = new UserGroupViewDto();
        viewDto.publicId = "gp1";
        viewDto.name = "Math Class";

        final var dto = viewDto.toUserGroupDto();
        assertEquals("gp1", dto.publicId);
        assertEquals("Math Class", dto.name);
    }
}
