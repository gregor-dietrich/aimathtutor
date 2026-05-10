package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.LessonEntity;

class LessonViewDtoTest {

    @Test
    @DisplayName("Default constructor has null fields")
    void testDefaultConstructor() {
        final var dto = new LessonViewDto();
        assertNull(dto.publicId);
        assertNull(dto.name);
        assertFalse(dto.isRootLesson);
        assertEquals(0, dto.childrenCount);
        assertEquals(0, dto.exercisesCount);
    }

    @Test
    @DisplayName("Constructor from null entity returns default values")
    void testConstructorFromNull() {
        final var dto = new LessonViewDto(null);
        assertNull(dto.publicId);
        assertNull(dto.name);
    }

    @Test
    @DisplayName("Constructor from root lesson without children")
    void testConstructorFromRootLesson() {
        final var lesson = new LessonEntity();
        lesson.publicId = "lesson-1";
        lesson.name = "Root Lesson";
        lesson.parent = null;

        final var dto = new LessonViewDto(lesson);
        assertEquals("lesson-1", dto.publicId);
        assertEquals("Root Lesson", dto.name);
        assertTrue(dto.isRootLesson());
        assertEquals(0, dto.childrenCount);
        assertEquals(List.of(), dto.childrenPublicIds);
    }

    @Test
    @DisplayName("Constructor from child lesson maps parent fields")
    void testConstructorFromChildLesson() {
        final var parent = new LessonEntity();
        parent.publicId = "parent-1";
        parent.name = "Parent Lesson";

        final var lesson = new LessonEntity();
        lesson.publicId = "lesson-2";
        lesson.name = "Child Lesson";
        lesson.parent = parent;

        final var dto = new LessonViewDto(lesson);
        assertEquals("parent-1", dto.parentPublicId);
        assertEquals("Parent Lesson", dto.parentName);
        assertFalse(dto.isRootLesson());
    }

    @Test
    @DisplayName("Constructor from lesson with children counts them")
    void testConstructorWithChildren() {
        final var child1 = new LessonEntity();
        child1.publicId = "child-1";
        final var child2 = new LessonEntity();
        child2.publicId = "child-2";

        final var lesson = new LessonEntity();
        lesson.publicId = "parent-1";
        lesson.name = "Parent";
        lesson.children = List.of(child1, child2);

        final var dto = new LessonViewDto(lesson);
        assertEquals(2, dto.childrenCount);
        assertEquals(List.of("child-1", "child-2"), dto.childrenPublicIds);
    }

    @Test
    @DisplayName("Constructor from lesson with exercises counts published only")
    void testConstructorWithExercises() {
        final var ex1 = new ExerciseEntity();
        ex1.published = true;
        final var ex2 = new ExerciseEntity();
        ex2.published = false;
        final var ex3 = new ExerciseEntity();
        ex3.published = true;

        final var lesson = new LessonEntity();
        lesson.publicId = "lesson-1";
        lesson.name = "Lesson";
        lesson.exercises = List.of(ex1, ex2, ex3);

        final var dto = new LessonViewDto(lesson);
        assertEquals(2, dto.exercisesCount);
    }

    @Test
    @DisplayName("getName returns the name")
    void testGetName() {
        final var dto = new LessonViewDto();
        dto.name = "Algebra";
        assertEquals("Algebra", dto.getName());
    }

    @Test
    @DisplayName("getPublicId returns the publicId")
    void testGetPublicId() {
        final var dto = new LessonViewDto();
        dto.publicId = "pub-123";
        assertEquals("pub-123", dto.getPublicId());
    }

    @Test
    @DisplayName("toLessonDto maps fields correctly")
    void testToLessonDto() {
        final var dto = new LessonViewDto();
        dto.publicId = "lesson-1";
        dto.name = "Algebra";
        dto.parentPublicId = "parent-1";

        final var result = dto.toLessonDto();
        assertNotNull(result);
        assertEquals("lesson-1", result.publicId);
        assertEquals("Algebra", result.name);
        assertEquals("parent-1", result.parentPublicId);
        assertNotNull(result.parent);
        assertEquals("parent-1", result.parent.publicId);
    }

    @Test
    @DisplayName("toLessonDto with null parent does not set parent field")
    void testToLessonDtoNoParent() {
        final var dto = new LessonViewDto();
        dto.publicId = "lesson-1";
        dto.name = "Root";
        dto.parentPublicId = null;

        final var result = dto.toLessonDto();
        assertNull(result.parent);
        assertNull(result.parentPublicId);
    }
}
