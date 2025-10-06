package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.LessonViewDto;
import de.vptr.aimathtutor.entity.LessonEntity;

class LessonViewDtoTest {

    private LessonEntity lessonEntity;
    private LessonEntity parentEntity;

    @BeforeEach
    void setUp() {
        this.parentEntity = new LessonEntity();
        this.parentEntity.id = 1L;
        this.parentEntity.name = "Parent Lesson";
        this.parentEntity.parent = null;

        this.lessonEntity = new LessonEntity();
        this.lessonEntity.id = 2L;
        this.lessonEntity.name = "Test Lesson";
        this.lessonEntity.parent = this.parentEntity;
    }

    @Test
    @DisplayName("Should create DTO from entity")
    void shouldCreateDtoFromEntity() {
        final LessonViewDto dto = new LessonViewDto(this.lessonEntity);

        assertEquals(2L, dto.id);
        assertEquals("Test Lesson", dto.name);
        assertEquals(1L, dto.parentId);
    }

    @Test
    @DisplayName("Should handle null parent")
    void shouldHandleNullParent() {
        this.lessonEntity.parent = null;
        final LessonViewDto dto = new LessonViewDto(this.lessonEntity);

        assertEquals(2L, dto.id);
        assertEquals("Test Lesson", dto.name);
        assertNull(dto.parentId);
    }

    @Test
    @DisplayName("Should handle root lesson")
    void shouldHandleRootLesson() {
        final LessonViewDto dto = new LessonViewDto(this.parentEntity);

        assertEquals(1L, dto.id);
        assertEquals("Parent Lesson", dto.name);
        assertNull(dto.parentId);
    }
}
