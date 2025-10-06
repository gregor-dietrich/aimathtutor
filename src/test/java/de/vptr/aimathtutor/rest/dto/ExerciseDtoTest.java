package de.vptr.aimathtutor.rest.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExerciseDtoTest {

    private ExerciseDto exerciseDto;

    @BeforeEach
    void setUp() {
        this.exerciseDto = new ExerciseDto();
    }

    @Test
    @DisplayName("Should create ExerciseDto with default constructor")
    void shouldCreateExerciseDtoWithDefaultConstructor() {
        // Then
        assertNull(this.exerciseDto.id);
        assertNull(this.exerciseDto.title);
        assertNull(this.exerciseDto.content);
        assertNull(this.exerciseDto.userId);
        assertNull(this.exerciseDto.lessonId);
        assertNull(this.exerciseDto.published);
        assertNull(this.exerciseDto.commentable);
        assertNull(this.exerciseDto.created);
        assertNull(this.exerciseDto.lastEdit);
        assertNull(this.exerciseDto.user);
        assertNull(this.exerciseDto.lesson);
    }

    @Test
    @DisplayName("Should create ExerciseDto with parameterized constructor")
    void shouldCreateExerciseDtoWithParameterizedConstructor() {
        // When
        final ExerciseDto dto = new ExerciseDto("Test Title", "Test Content", 1L, 2L, true, false);

        // Then
        assertEquals("Test Title", dto.title);
        assertEquals("Test Content", dto.content);
        assertEquals(1L, dto.userId);
        assertEquals(2L, dto.lessonId);
        assertTrue(dto.published);
        assertFalse(dto.commentable);
        assertNull(dto.id); // Not set by constructor
        assertNull(dto.created);
        assertNull(dto.lastEdit);
    }

    @Test
    @DisplayName("Should set all fields correctly")
    void shouldSetAllFieldsCorrectly() {
        // Given
        final LocalDateTime now = LocalDateTime.now();

        // When
        this.exerciseDto.id = 1L;
        this.exerciseDto.title = "Test Exercise";
        this.exerciseDto.content = "This is test content";
        this.exerciseDto.userId = 2L;
        this.exerciseDto.lessonId = 3L;
        this.exerciseDto.published = true;
        this.exerciseDto.commentable = false;
        this.exerciseDto.created = now;
        this.exerciseDto.lastEdit = now;

        // Then
        assertEquals(1L, this.exerciseDto.id);
        assertEquals("Test Exercise", this.exerciseDto.title);
        assertEquals("This is test content", this.exerciseDto.content);
        assertEquals(2L, this.exerciseDto.userId);
        assertEquals(3L, this.exerciseDto.lessonId);
        assertTrue(this.exerciseDto.published);
        assertFalse(this.exerciseDto.commentable);
        assertEquals(now, this.exerciseDto.created);
        assertEquals(now, this.exerciseDto.lastEdit);
    }

    @Test
    @DisplayName("Should sync nested user field correctly")
    void shouldSyncNestedUserFieldCorrectly() {
        // Given
        this.exerciseDto.userId = 1L;

        // When
        this.exerciseDto.syncNestedFields();

        // Then
        assertNotNull(this.exerciseDto.user);
        assertEquals(1L, this.exerciseDto.user.id);

        // Given - Set user object
        final ExerciseDto.UserField userField = new ExerciseDto.UserField(2L);
        userField.username = "testuser";
        this.exerciseDto.user = userField;
        this.exerciseDto.userId = null;

        // When
        this.exerciseDto.syncNestedFields();

        // Then
        assertEquals(2L, this.exerciseDto.userId);
    }

    @Test
    @DisplayName("Should sync nested lesson field correctly")
    void shouldSyncNestedLessonFieldCorrectly() {
        // Given
        this.exerciseDto.lessonId = 1L;

        // When
        this.exerciseDto.syncNestedFields();

        // Then
        assertNotNull(this.exerciseDto.lesson);
        assertEquals(1L, this.exerciseDto.lesson.id);

        // Given - Set lesson object
        final ExerciseDto.LessonField lessonField = new ExerciseDto.LessonField(2L);
        lessonField.name = "Test Lesson";
        this.exerciseDto.lesson = lessonField;
        this.exerciseDto.lessonId = null;

        // When
        this.exerciseDto.syncNestedFields();

        // Then
        assertEquals(2L, this.exerciseDto.lessonId);
    }

    @Test
    @DisplayName("Should handle UserField operations")
    void shouldHandleUserFieldOperations() {
        // Test default constructor
        final ExerciseDto.UserField userField = new ExerciseDto.UserField();
        assertNull(userField.id);
        assertNull(userField.username);

        // Test parameterized constructor
        final ExerciseDto.UserField userField2 = new ExerciseDto.UserField(1L);
        assertEquals(1L, userField2.id);
        assertNull(userField2.username);

        // Test setters
        userField.setId(2L);
        userField.setUsername("testuser");
        assertEquals(2L, userField.id);
        assertEquals("testuser", userField.username);
    }

    @Test
    @DisplayName("Should handle LessonField operations")
    void shouldHandleLessonFieldOperations() {
        // Test default constructor
        final ExerciseDto.LessonField lessonField = new ExerciseDto.LessonField();
        assertNull(lessonField.id);
        assertNull(lessonField.name);

        // Test parameterized constructor
        final ExerciseDto.LessonField lessonField2 = new ExerciseDto.LessonField(1L);
        assertEquals(1L, lessonField2.id);
        assertNull(lessonField2.name);

        // Test setting fields
        lessonField.id = 2L;
        lessonField.name = "Test Lesson";
        assertEquals(2L, lessonField.id);
        assertEquals("Test Lesson", lessonField.name);
    }

    @Test
    @DisplayName("Should handle null values in sync")
    void shouldHandleNullValuesInSync() {
        // Given - all null
        this.exerciseDto.userId = null;
        this.exerciseDto.lessonId = null;
        this.exerciseDto.user = null;
        this.exerciseDto.lesson = null;

        // When
        this.exerciseDto.syncNestedFields();

        // Then - should remain null
        assertNull(this.exerciseDto.userId);
        assertNull(this.exerciseDto.lessonId);
        assertNull(this.exerciseDto.user);
        assertNull(this.exerciseDto.lesson);
    }

    @Test
    @DisplayName("Should handle long content")
    void shouldHandleLongContent() {
        // Given
        final StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longContent.append("This is a very long exercise content. ");
        }

        // When
        this.exerciseDto.content = longContent.toString();

        // Then
        assertEquals(longContent.toString(), this.exerciseDto.content);
        assertTrue(this.exerciseDto.content.length() > 10000);
    }

    @Test
    @DisplayName("Should handle special characters in content")
    void shouldHandleSpecialCharactersInContent() {
        // Given
        final String specialContent = "Content with special chars: @#$%^&*()_+-={}[]|\\:;\"'<>,.?/~`\nNew line\tTab";

        // When
        this.exerciseDto.title = "Title with émojis 🚀 and ünicöde";
        this.exerciseDto.content = specialContent;

        // Then
        assertEquals("Title with émojis 🚀 and ünicöde", this.exerciseDto.title);
        assertEquals(specialContent, this.exerciseDto.content);
    }

    @Test
    @DisplayName("Should handle HTML content")
    void shouldHandleHtmlContent() {
        // Given
        final String htmlContent = "<h1>Title</h1><p>Paragraph with <strong>bold</strong> and <em>italic</em> text.</p><ul><li>Item 1</li><li>Item 2</li></ul>";

        // When
        this.exerciseDto.content = htmlContent;

        // Then
        assertEquals(htmlContent, this.exerciseDto.content);
    }

    @Test
    @DisplayName("Should handle markdown content")
    void shouldHandleMarkdownContent() {
        // Given
        final String markdownContent = "# Title\n\nThis is a paragraph with **bold** and *italic* text.\n\n- Item 1\n- Item 2\n\n```java\nSystem.out.println(\"Hello World\");\n```";

        // When
        this.exerciseDto.content = markdownContent;

        // Then
        assertEquals(markdownContent, this.exerciseDto.content);
    }
}