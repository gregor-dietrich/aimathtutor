package de.vptr.aimathtutor.rest.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LessonEntityTest {

    private LessonEntity lesson;
    private LessonEntity parentLesson;

    @BeforeEach
    void setUp() {
        this.lesson = new LessonEntity();
        this.parentLesson = new LessonEntity();
        this.parentLesson.id = 1L;
        this.parentLesson.name = "Parent Lesson";
    }

    @Test
    @DisplayName("Should create LessonEntity with all fields")
    void shouldCreateLessonEntityWithAllFields() {
        // Given
        final List<LessonEntity> children = new ArrayList<>();
        final List<ExerciseEntity> exercises = new ArrayList<>();

        // When
        this.lesson.id = 2L;
        this.lesson.name = "Child Lesson";
        this.lesson.parent = this.parentLesson;
        this.lesson.children = children;
        this.lesson.exercises = exercises;

        // Then
        assertEquals(2L, this.lesson.id);
        assertEquals("Child Lesson", this.lesson.name);
        assertEquals(this.parentLesson, this.lesson.parent);
        assertEquals(children, this.lesson.children);
        assertEquals(exercises, this.lesson.exercises);
    }

    @Test
    @DisplayName("Should identify root lesson correctly")
    void shouldIdentifyRootLessonCorrectly() {
        // Given - lesson without parent
        this.lesson.parent = null;

        // When & Then
        assertTrue(this.lesson.isRootLesson());

        // Given - lesson with parent
        this.lesson.parent = this.parentLesson;

        // When & Then
        assertFalse(this.lesson.isRootLesson());
    }

    @Test
    @DisplayName("Should handle children collection properly")
    void shouldHandleChildrenCollectionProperly() {
        // Given
        final List<LessonEntity> children = new ArrayList<>();
        final LessonEntity child1 = new LessonEntity();
        child1.id = 3L;
        child1.name = "Child 1";

        final LessonEntity child2 = new LessonEntity();
        child2.id = 4L;
        child2.name = "Child 2";

        children.add(child1);
        children.add(child2);

        // When
        this.lesson.children = children;

        // Then
        assertNotNull(this.lesson.children);
        assertEquals(2, this.lesson.children.size());
        assertEquals(child1, this.lesson.children.get(0));
        assertEquals(child2, this.lesson.children.get(1));
    }

    @Test
    @DisplayName("Should handle exercises collection properly")
    void shouldHandleExercisesCollectionProperly() {
        // Given
        final List<ExerciseEntity> exercises = new ArrayList<>();
        final ExerciseEntity exercise1 = new ExerciseEntity();
        exercise1.id = 1L;
        exercise1.title = "Exercise 1";

        final ExerciseEntity exercise2 = new ExerciseEntity();
        exercise2.id = 2L;
        exercise2.title = "Exercise 2";

        exercises.add(exercise1);
        exercises.add(exercise2);

        // When
        this.lesson.exercises = exercises;

        // Then
        assertNotNull(this.lesson.exercises);
        assertEquals(2, this.lesson.exercises.size());
        assertEquals(exercise1, this.lesson.exercises.get(0));
        assertEquals(exercise2, this.lesson.exercises.get(1));
    }

    @Test
    @DisplayName("Should handle empty collections")
    void shouldHandleEmptyCollections() {
        // When
        this.lesson.children = new ArrayList<>();
        this.lesson.exercises = new ArrayList<>();

        // Then
        assertNotNull(this.lesson.children);
        assertNotNull(this.lesson.exercises);
        assertTrue(this.lesson.children.isEmpty());
        assertTrue(this.lesson.exercises.isEmpty());
    }

    @Test
    @DisplayName("Should handle null parent")
    void shouldHandleNullParent() {
        // When
        this.lesson.parent = null;

        // Then
        assertNull(this.lesson.parent);
        assertTrue(this.lesson.isRootLesson());
    }

    @Test
    @DisplayName("Should handle null collections")
    void shouldHandleNullCollections() {
        // When
        this.lesson.children = null;
        this.lesson.exercises = null;

        // Then
        assertNull(this.lesson.children);
        assertNull(this.lesson.exercises);
    }

    @Test
    @DisplayName("Should create hierarchical structure")
    void shouldCreateHierarchicalStructure() {
        // Given
        final LessonEntity grandparent = new LessonEntity();
        grandparent.id = 1L;
        grandparent.name = "Grandparent";
        grandparent.parent = null;

        final LessonEntity parent = new LessonEntity();
        parent.id = 2L;
        parent.name = "Parent";
        parent.parent = grandparent;

        final LessonEntity child = new LessonEntity();
        child.id = 3L;
        child.name = "Child";
        child.parent = parent;

        // Then
        assertTrue(grandparent.isRootLesson());
        assertFalse(parent.isRootLesson());
        assertFalse(child.isRootLesson());

        assertNull(grandparent.parent);
        assertEquals(grandparent, parent.parent);
        assertEquals(parent, child.parent);
    }
}