package de.vptr.aimathtutor.rest.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.rest.entity.ExerciseEntity;
import de.vptr.aimathtutor.rest.entity.LessonEntity;
import de.vptr.aimathtutor.rest.entity.UserEntity;

class ExerciseViewDtoTest {

    private ExerciseEntity exerciseEntity;
    private UserEntity userEntity;
    private LessonEntity lessonEntity;

    @BeforeEach
    void setUp() {
        this.userEntity = new UserEntity();
        this.userEntity.id = 1L;
        this.userEntity.username = "testuser";

        this.lessonEntity = new LessonEntity();
        this.lessonEntity.id = 5L;
        this.lessonEntity.name = "Test Lesson";

        this.exerciseEntity = new ExerciseEntity();
        this.exerciseEntity.id = 10L;
        this.exerciseEntity.title = "Test Exercise";
        this.exerciseEntity.content = "Test exercise content";
        this.exerciseEntity.published = true;
        this.exerciseEntity.commentable = true;
        this.exerciseEntity.created = LocalDateTime.of(2025, 10, 1, 12, 0);
        this.exerciseEntity.lastEdit = LocalDateTime.of(2025, 10, 1, 13, 0);
        this.exerciseEntity.user = this.userEntity;
        this.exerciseEntity.lesson = this.lessonEntity;
    }

    @Test
    @DisplayName("Should create DTO from entity")
    void shouldCreateDtoFromEntity() {
        final ExerciseViewDto dto = new ExerciseViewDto(this.exerciseEntity);

        assertEquals(10L, dto.id);
        assertEquals("Test Exercise", dto.title);
        assertEquals("Test exercise content", dto.content);
        assertTrue(dto.published);
        assertTrue(dto.commentable);
        assertEquals(LocalDateTime.of(2025, 10, 1, 12, 0), dto.created);
        assertEquals(LocalDateTime.of(2025, 10, 1, 13, 0), dto.lastEdit);
        assertEquals(1L, dto.userId);
        assertEquals("testuser", dto.username);
        assertEquals(5L, dto.lessonId);
        assertEquals("Test Lesson", dto.lessonName);
    }

    @Test
    @DisplayName("Should handle null user")
    void shouldHandleNullUser() {
        this.exerciseEntity.user = null;
        final ExerciseViewDto dto = new ExerciseViewDto(this.exerciseEntity);

        assertNull(dto.userId);
        assertNull(dto.username);
    }

    @Test
    @DisplayName("Should handle null lesson")
    void shouldHandleNullLesson() {
        this.exerciseEntity.lesson = null;
        final ExerciseViewDto dto = new ExerciseViewDto(this.exerciseEntity);

        assertNull(dto.lessonId);
        assertNull(dto.lessonName);
    }

    @Test
    @DisplayName("Should handle unpublished exercise")
    void shouldHandleUnpublishedExercise() {
        this.exerciseEntity.published = false;
        final ExerciseViewDto dto = new ExerciseViewDto(this.exerciseEntity);

        assertFalse(dto.published);
    }

    @Test
    @DisplayName("Should handle non-commentable exercise")
    void shouldHandleNonCommentableExercise() {
        this.exerciseEntity.commentable = false;
        final ExerciseViewDto dto = new ExerciseViewDto(this.exerciseEntity);

        assertFalse(dto.commentable);
    }
}
