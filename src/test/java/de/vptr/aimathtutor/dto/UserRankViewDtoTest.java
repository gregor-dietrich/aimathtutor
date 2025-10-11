package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.UserRankEntity;

class UserRankViewDtoTest {

    private UserRankEntity rankEntity;

    @BeforeEach
    void setUp() {
        this.rankEntity = new UserRankEntity();
        this.rankEntity.id = 1L;
        this.rankEntity.name = "Admin";
        this.rankEntity.adminView = true;
        this.rankEntity.lessonAdd = true;
        this.rankEntity.lessonEdit = true;
        this.rankEntity.lessonDelete = false;
        this.rankEntity.exerciseAdd = true;
        this.rankEntity.exerciseEdit = true;
        this.rankEntity.exerciseDelete = false;
    }

    @Test
    @DisplayName("Should create DTO from entity")
    void shouldCreateDtoFromEntity() {
        final UserRankViewDto dto = new UserRankViewDto(this.rankEntity);

        assertEquals(1L, dto.id);
        assertEquals("Admin", dto.name);
        assertTrue(dto.adminView);
        assertTrue(dto.lessonAdd);
        assertTrue(dto.lessonEdit);
        assertFalse(dto.lessonDelete);
        assertTrue(dto.exerciseAdd);
        assertTrue(dto.exerciseEdit);
        assertFalse(dto.exerciseDelete);
    }

    @Test
    @DisplayName("Should handle entity with all permissions false")
    void shouldHandleEntityWithAllPermissionsFalse() {
        this.rankEntity.adminView = false;
        this.rankEntity.lessonAdd = false;
        this.rankEntity.lessonEdit = false;
        this.rankEntity.exerciseAdd = false;
        this.rankEntity.exerciseEdit = false;

        final UserRankViewDto dto = new UserRankViewDto(this.rankEntity);

        assertFalse(dto.adminView);
        assertFalse(dto.lessonAdd);
        assertFalse(dto.lessonEdit);
        assertFalse(dto.exerciseAdd);
        assertFalse(dto.exerciseEdit);
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
