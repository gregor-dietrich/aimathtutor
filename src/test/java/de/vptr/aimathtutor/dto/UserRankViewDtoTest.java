package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.UserRankViewDto;
import de.vptr.aimathtutor.entity.UserRankEntity;

class UserRankViewDtoTest {

    private UserRankEntity rankEntity;

    @BeforeEach
    void setUp() {
        this.rankEntity = new UserRankEntity();
        this.rankEntity.id = 1L;
        this.rankEntity.name = "Admin";
        this.rankEntity.adminView = true;
        this.rankEntity.pageAdd = true;
        this.rankEntity.pageEdit = true;
        this.rankEntity.pageDelete = false;
        this.rankEntity.postAdd = true;
        this.rankEntity.postEdit = true;
        this.rankEntity.postDelete = false;
    }

    @Test
    @DisplayName("Should create DTO from entity")
    void shouldCreateDtoFromEntity() {
        final UserRankViewDto dto = new UserRankViewDto(this.rankEntity);

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
        this.rankEntity.adminView = false;
        this.rankEntity.pageAdd = false;
        this.rankEntity.pageEdit = false;
        this.rankEntity.postAdd = false;
        this.rankEntity.postEdit = false;

        final UserRankViewDto dto = new UserRankViewDto(this.rankEntity);

        assertFalse(dto.adminView);
        assertFalse(dto.pageAdd);
        assertFalse(dto.pageEdit);
        assertFalse(dto.postAdd);
        assertFalse(dto.postEdit);
    }

    @Test
    @DisplayName("Should handle entity with null name")
    void shouldHandleEntityWithNullName() {
        this.rankEntity.name = null;
        final UserRankViewDto dto = new UserRankViewDto(this.rankEntity);

        assertEquals(1L, dto.id);
        assertNull(dto.name);
    }
}
