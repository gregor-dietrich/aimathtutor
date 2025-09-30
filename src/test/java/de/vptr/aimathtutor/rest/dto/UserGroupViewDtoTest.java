package de.vptr.aimathtutor.rest.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.rest.entity.UserGroupEntity;

class UserGroupViewDtoTest {

    private UserGroupEntity groupEntity;

    @BeforeEach
    void setUp() {
        groupEntity = new UserGroupEntity();
        groupEntity.id = 1L;
        groupEntity.name = "Test Group";
    }

    @Test
    @DisplayName("Should create DTO from entity")
    void shouldCreateDtoFromEntity() {
        UserGroupViewDto dto = new UserGroupViewDto(groupEntity);

        assertEquals(1L, dto.id);
        assertEquals("Test Group", dto.name);
    }

    @Test
    @DisplayName("Should handle entity with null name")
    void shouldHandleEntityWithNullName() {
        groupEntity.name = null;
        UserGroupViewDto dto = new UserGroupViewDto(groupEntity);

        assertEquals(1L, dto.id);
        assertNull(dto.name);
    }

    @Test
    @DisplayName("Should handle entity with empty name")
    void shouldHandleEntityWithEmptyName() {
        groupEntity.name = "";
        UserGroupViewDto dto = new UserGroupViewDto(groupEntity);

        assertEquals(1L, dto.id);
        assertEquals("", dto.name);
    }
}
