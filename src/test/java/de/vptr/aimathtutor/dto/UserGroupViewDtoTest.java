package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.UserGroupEntity;

class UserGroupViewDtoTest {

    private UserGroupEntity groupEntity;

    @BeforeEach
    void setUp() {
        this.groupEntity = new UserGroupEntity();
        this.groupEntity.id = 1L;
        this.groupEntity.name = "Test Group";
    }

    @Test
    @DisplayName("Should create DTO from entity")
    void shouldCreateDtoFromEntity() {
        final UserGroupViewDto dto = new UserGroupViewDto(this.groupEntity);

        assertEquals(1L, dto.id);
        assertEquals("Test Group", dto.name);
    }

    @Test
    @DisplayName("Should handle entity with null name")
    void shouldHandleEntityWithNullName() {
        this.groupEntity.name = null;
        final UserGroupViewDto dto = new UserGroupViewDto(this.groupEntity);

        assertEquals(1L, dto.id);
        assertNull(dto.name);
    }

    @Test
    @DisplayName("Should handle entity with empty name")
    void shouldHandleEntityWithEmptyName() {
        this.groupEntity.name = "";
        final UserGroupViewDto dto = new UserGroupViewDto(this.groupEntity);

        assertEquals(1L, dto.id);
        assertEquals("", dto.name);
    }
}
