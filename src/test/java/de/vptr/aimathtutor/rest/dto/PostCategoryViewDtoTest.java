package de.vptr.aimathtutor.rest.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.rest.entity.PostCategoryEntity;

class PostCategoryViewDtoTest {

    private PostCategoryEntity categoryEntity;
    private PostCategoryEntity parentEntity;

    @BeforeEach
    void setUp() {
        parentEntity = new PostCategoryEntity();
        parentEntity.id = 1L;
        parentEntity.name = "Parent Category";
        parentEntity.parent = null;

        categoryEntity = new PostCategoryEntity();
        categoryEntity.id = 2L;
        categoryEntity.name = "Test Category";
        categoryEntity.parent = parentEntity;
    }

    @Test
    @DisplayName("Should create DTO from entity")
    void shouldCreateDtoFromEntity() {
        PostCategoryViewDto dto = new PostCategoryViewDto(categoryEntity);

        assertEquals(2L, dto.id);
        assertEquals("Test Category", dto.name);
        assertEquals(1L, dto.parentId);
    }

    @Test
    @DisplayName("Should handle null parent")
    void shouldHandleNullParent() {
        categoryEntity.parent = null;
        PostCategoryViewDto dto = new PostCategoryViewDto(categoryEntity);

        assertEquals(2L, dto.id);
        assertEquals("Test Category", dto.name);
        assertNull(dto.parentId);
    }

    @Test
    @DisplayName("Should handle root category")
    void shouldHandleRootCategory() {
        PostCategoryViewDto dto = new PostCategoryViewDto(parentEntity);

        assertEquals(1L, dto.id);
        assertEquals("Parent Category", dto.name);
        assertNull(dto.parentId);
    }
}
