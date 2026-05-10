package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.ExerciseDto.DifficultyLevel;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.LessonEntity;
import de.vptr.aimathtutor.entity.UserEntity;

@SuppressWarnings("NullAway")
class ExerciseViewDtoTest {

    @Test
    @DisplayName("Default constructor has null fields")
    void testDefaultConstructor() {
        final var dto = new ExerciseViewDto();
        assertNull(dto.publicId);
        assertNull(dto.title);
        assertNull(dto.content);
    }

    @Test
    @DisplayName("Constructor from null entity yields default values")
    void testConstructorFromNull() {
        final var dto = new ExerciseViewDto(null);
        assertNull(dto.id);
        assertNull(dto.publicId);
        assertNull(dto.title);
    }

    @Test
    @DisplayName("Constructor from entity maps basic fields")
    void testConstructorFromEntity() {
        final var entity = new ExerciseEntity();
        entity.id = 42L;
        entity.publicId = "ex-pub-1";
        entity.title = "Test Exercise";
        entity.content = "Solve 2x+5=15";
        entity.published = true;
        entity.commentable = true;
        entity.graspableEnabled = true;
        entity.graspableInitialExpression = "2x+5=15";
        entity.graspableTargetExpression = "x=5";
        entity.graspableDifficulty = DifficultyLevel.INTERMEDIATE;
        entity.graspableHints = "[\"Hint 1\"]";

        final var dto = new ExerciseViewDto(entity);
        assertEquals(42L, dto.id);
        assertEquals("ex-pub-1", dto.publicId);
        assertEquals("Test Exercise", dto.title);
        assertEquals("Solve 2x+5=15", dto.content);
        assertEquals(true, dto.published);
        assertEquals(true, dto.commentable);
        assertEquals(true, dto.graspableEnabled);
        assertEquals("2x+5=15", dto.graspableInitialExpression);
        assertEquals("x=5", dto.graspableTargetExpression);
        assertEquals(DifficultyLevel.INTERMEDIATE, dto.graspableDifficulty);
        assertEquals("[\"Hint 1\"]", dto.graspableHints);
        assertEquals(0L, dto.commentsCount);
    }

    @Test
    @DisplayName("Constructor from entity with user and lesson maps relationships")
    void testConstructorFromEntityWithRelations() {
        final var user = new UserEntity();
        user.publicId = "user-1";
        user.username = "testuser";

        final var lesson = new LessonEntity();
        lesson.publicId = "lesson-1";
        lesson.name = "Algebra";

        final var entity = new ExerciseEntity();
        entity.publicId = "ex-1";
        entity.title = "Test";
        entity.content = "Content";
        entity.user = user;
        entity.lesson = lesson;

        final var dto = new ExerciseViewDto(entity);
        assertEquals("user-1", dto.userPublicId);
        assertEquals("testuser", dto.username);
        assertEquals("lesson-1", dto.lessonPublicId);
        assertEquals("Algebra", dto.lessonName);
    }

    @Test
    @DisplayName("Constructor from entity with null relations maps nulls")
    void testConstructorFromEntityNullRelations() {
        final var entity = new ExerciseEntity();
        entity.publicId = "ex-2";
        entity.title = "No Relations";
        entity.content = "Content";

        final var dto = new ExerciseViewDto(entity);
        assertNull(dto.userPublicId);
        assertNull(dto.username);
        assertNull(dto.lessonPublicId);
        assertNull(dto.lessonName);
    }

    @Test
    @DisplayName("getPublicId returns publicId")
    void testGetPublicId() {
        final var dto = new ExerciseViewDto();
        dto.publicId = "ex-123";
        assertEquals("ex-123", dto.getPublicId());
    }

    @Test
    @DisplayName("toExerciseDto converts correctly")
    void testToExerciseDto() {
        final var dto = new ExerciseViewDto();
        dto.publicId = "ex-1";
        dto.title = "Title";
        dto.content = "Content";
        dto.userPublicId = "user-1";
        dto.lessonPublicId = "lesson-1";
        dto.published = true;
        dto.commentable = false;
        dto.graspableEnabled = true;
        dto.graspableInitialExpression = "2x=10";
        dto.graspableTargetExpression = "x=5";
        dto.graspableDifficulty = DifficultyLevel.BEGINNER;
        dto.graspableHints = "[\"Hint\"]";

        final var result = dto.toExerciseDto();
        assertNotNull(result);
        assertEquals("ex-1", result.publicId);
        assertEquals("Title", result.title);
        assertEquals("Content", result.content);
        assertEquals("user-1", result.userPublicId);
        assertEquals("lesson-1", result.lessonPublicId);
        assertEquals(true, result.published);
        assertEquals(false, result.commentable);
        assertEquals(true, result.graspableEnabled);
        assertEquals("2x=10", result.graspableInitialExpression);
        assertEquals("x=5", result.graspableTargetExpression);
        assertEquals(DifficultyLevel.BEGINNER, result.graspableDifficulty);
    }
}
