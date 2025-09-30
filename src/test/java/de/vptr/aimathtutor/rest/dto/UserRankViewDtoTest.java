package de.vptr.aimathtutor.rest.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.rest.entity.UserRankEntity;

class UserRankViewDtoTest {

    private UserRankEntity rankEntity;

    @BeforeEach
    void setUp() {
        rankEntity = new UserRankEntity();
        rankEntity.id = 1L;
        rankEntity.name = "Admin";
        rankEntity.adminView = true;
        rankEntity.pageAdd = true;
        rankEntity.pageEdit = true;
        rankEntity.pageDelete = false;
        rankEntity.postAdd = true;
        rankEntity.postEdit = true;
        rankEntity.postDelete = false;
    }

    @Test
    @DisplayName("Should create DTO from entity")
    void shouldCreateDtoFromEntity() {
        UserRankViewDto dto = new UserRankViewDto(rankEntity);

        assertEquals(1L, dto.id);
        assertEquals("Admin", dto.name);
        assertTrue(dto.adminView);
        assertTrue(dto.pageAdd);
        assertTrue(dto.pageEdit);
        assertFalse(dto.pageDelete);
        assertTrue(dto.postAdd);
        assertTrue(dto.postEdit);
        assertFalse(dto.postDelete);
    }

    @Test
    @DisplayName("Should handle entity with all permissions false")
    void shouldHandleEntityWithAllPermissionsFalse() {
        rankEntity.adminView = false;
        rankEntity.pageAdd = false;
        rankEntity.pageEdit = false;
        rankEntity.postAdd = false;
        rankEntity.postEdit = false;

        UserRankViewDto dto = new UserRankViewDto(rankEntity);

        assertFalse(dto.adminView);
        assertFalse(dto.pageAdd);
        assertFalse(dto.pageEdit);
        assertFalse(dto.postAdd);
        assertFalse(dto.postEdit);
    }

    @Test
    @DisplayName("Should handle entity with null name")
    void shouldHandleEntityWithNullName() {
        rankEntity.name = null;
        UserRankViewDto dto = new UserRankViewDto(rankEntity);

        assertEquals(1L, dto.id);
        assertNull(dto.name);
    }
}
