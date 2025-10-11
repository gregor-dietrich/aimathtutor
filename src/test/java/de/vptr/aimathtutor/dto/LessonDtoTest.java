package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LessonDtoTest {

    private LessonDto lessonDto;

    @BeforeEach
    void setUp() {
        this.lessonDto = new LessonDto();
    }

    @Test
    @DisplayName("Should create LessonDto with default constructor")
    void shouldCreateLessonDtoWithDefaultConstructor() {
        // Then
        assertNull(this.lessonDto.id);
        assertNull(this.lessonDto.name);
        assertNull(this.lessonDto.parentId);
        assertNull(this.lessonDto.parent);
    }

    @Test
    @DisplayName("Should set all fields correctly")
    void shouldSetAllFieldsCorrectly() {
        // When
        this.lessonDto.id = 1L;
        this.lessonDto.name = "Test Lesson";
        this.lessonDto.parentId = 2L;

        // Then
        assertEquals(1L, this.lessonDto.id);
        assertEquals("Test Lesson", this.lessonDto.name);
        assertEquals(2L, this.lessonDto.parentId);
    }

    @Test
    @DisplayName("Should sync parent field correctly")
    void shouldSyncParentFieldCorrectly() {
        // Given
        this.lessonDto.parentId = 1L;

        // When
        this.lessonDto.syncParent();

        // Then
        assertNotNull(this.lessonDto.parent);
        assertEquals(1L, this.lessonDto.parent.id);

        // Given - Set parent object
        final LessonDto.ParentField parentField = new LessonDto.ParentField(2L);
        this.lessonDto.parent = parentField;
        this.lessonDto.parentId = null;

        // When
        this.lessonDto.syncParent();

        // Then
        assertEquals(2L, this.lessonDto.parentId);
    }

    @Test
    @DisplayName("Should handle ParentField operations")
    void shouldHandleParentFieldOperations() {
        // Test default constructor
        final LessonDto.ParentField parentField = new LessonDto.ParentField();
        assertNull(parentField.id);

        // Test parameterized constructor
        final LessonDto.ParentField parentField2 = new LessonDto.ParentField(1L);
        assertEquals(1L, parentField2.id);

        // Test setting fields
        parentField.id = 2L;
        assertEquals(2L, parentField.id);
    }

    @Test
    @DisplayName("Should handle null values in sync")
    void shouldHandleNullValuesInSync() {
        // Given - all null
        this.lessonDto.parentId = null;
        this.lessonDto.parent = null;

        // When
        this.lessonDto.syncParent();

        // Then - should remain null
        assertNull(this.lessonDto.parentId);
        assertNull(this.lessonDto.parent);
    }

    @Test
    @DisplayName("Should handle root lesson creation")
    void shouldHandleRootLessonCreation() {
        // Given - root lesson has no parent
        this.lessonDto.name = "Root Lesson";
        this.lessonDto.parentId = null;

        // When
        this.lessonDto.syncParent();

        // Then
        assertEquals("Root Lesson", this.lessonDto.name);
        assertNull(this.lessonDto.parentId);
        assertNull(this.lessonDto.parent);
    }

    @Test
    @DisplayName("Should handle sub-lesson creation")
    void shouldHandleSubLessonCreation() {
        // Given - sub-lesson has parent
        this.lessonDto.name = "Sub Lesson";
        this.lessonDto.parentId = 1L;

        // When
        this.lessonDto.syncParent();

        // Then
        assertEquals("Sub Lesson", this.lessonDto.name);
        assertEquals(1L, this.lessonDto.parentId);
        assertNotNull(this.lessonDto.parent);
        assertEquals(1L, this.lessonDto.parent.id);
    }

    @Test
    @DisplayName("Should handle empty name")
    void shouldHandleEmptyName() {
        // When
        this.lessonDto.name = "";

        // Then
        assertEquals("", this.lessonDto.name);
    }

    @Test
    @DisplayName("Should handle null name")
    void shouldHandleNullName() {
        // When
        this.lessonDto.name = null;

        // Then
        assertNull(this.lessonDto.name);
    }

    @Test
    @DisplayName("Should handle special characters in name")
    void shouldHandleSpecialCharactersInName() {
        // Given
        final String specialName = "Lesson with Special-Chars_123 & émojis 🚀";

        // When
        this.lessonDto.name = specialName;

        // Then
        assertEquals(specialName, this.lessonDto.name);
    }

    @Test
    @DisplayName("Should handle long lesson names")
    void shouldHandleLongLessonNames() {
        // Given
        final StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            longName.append("c");
        }

        // When
        this.lessonDto.name = longName.toString();

        // Then
        assertEquals(longName.toString(), this.lessonDto.name);
        assertEquals(300, this.lessonDto.name.length());
    }

    @Test
    @DisplayName("Should handle lesson hierarchy scenarios")
    void shouldHandleLessonHierarchyScenarios() {
        // Scenario 1: Creating root lesson
        final LessonDto rootLesson = new LessonDto();
        rootLesson.id = 1L;
        rootLesson.name = "Root";
        rootLesson.parentId = null;
        rootLesson.syncParent();

        assertNull(rootLesson.parentId);
        assertNull(rootLesson.parent);

        // Scenario 2: Creating child lesson
        final LessonDto childLesson = new LessonDto();
        childLesson.id = 2L;
        childLesson.name = "Child";
        childLesson.parentId = 1L;
        childLesson.syncParent();

        assertEquals(1L, childLesson.parentId);
        assertNotNull(childLesson.parent);
        assertEquals(1L, childLesson.parent.id);

        // Scenario 3: Moving lesson to different parent
        childLesson.parentId = 3L;
        childLesson.parent = null; // Reset parent object
        childLesson.syncParent();

        assertEquals(3L, childLesson.parentId);
        assertEquals(3L, childLesson.parent.id);

        // Scenario 4: Making lesson a root lesson
        childLesson.parentId = null;
        childLesson.parent = null;
        childLesson.syncParent();

        assertNull(childLesson.parentId);
        assertNull(childLesson.parent);
    }

    @Test
    @DisplayName("Should handle parent field with null id")
    void shouldHandleParentFieldWithNullId() {
        // Given
        final LessonDto.ParentField parentField = new LessonDto.ParentField(null);
        this.lessonDto.parent = parentField;

        // When
        this.lessonDto.syncParent();

        // Then - parent field exists but has null id, so parentId should remain null
        assertNotNull(this.lessonDto.parent);
        assertNull(this.lessonDto.parent.id);
        assertNull(this.lessonDto.parentId);
    }
}