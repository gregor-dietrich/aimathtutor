package de.vptr.aimathtutor.rest.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommentDtoTest {

    private CommentDto commentDto;

    @BeforeEach
    void setUp() {
        this.commentDto = new CommentDto();
    }

    @Test
    @DisplayName("Should create CommentDto with default constructor")
    void shouldCreateCommentDtoWithDefaultConstructor() {
        // Then
        assertNull(this.commentDto.id);
        assertNull(this.commentDto.content);
        assertNull(this.commentDto.exerciseId);
        assertNull(this.commentDto.exercise);
    }

    @Test
    @DisplayName("Should set all fields correctly")
    void shouldSetAllFieldsCorrectly() {
        // When
        this.commentDto.id = 1L;
        this.commentDto.content = "This is a test comment";
        this.commentDto.exerciseId = 2L;

        // Then
        assertEquals(1L, this.commentDto.id);
        assertEquals("This is a test comment", this.commentDto.content);
        assertEquals(2L, this.commentDto.exerciseId);
    }

    @Test
    @DisplayName("Should sync exercise field correctly")
    void shouldSyncExerciseFieldCorrectly() {
        // Given
        this.commentDto.exerciseId = 1L;

        // When
        this.commentDto.syncExercise();

        // Then
        assertNotNull(this.commentDto.exercise);
        assertEquals(1L, this.commentDto.exercise.id);

        // Given - Set exercise object
        final CommentDto.ExerciseField exerciseField = new CommentDto.ExerciseField(2L);
        this.commentDto.exercise = exerciseField;
        this.commentDto.exerciseId = null;

        // When
        this.commentDto.syncExercise();

        // Then
        assertEquals(2L, this.commentDto.exerciseId);
    }

    @Test
    @DisplayName("Should handle Exercise operations")
    void shouldHandleExerciseFieldOperations() {
        // Test default constructor
        final CommentDto.ExerciseField exerciseField = new CommentDto.ExerciseField();
        assertNull(exerciseField.id);

        // Test parameterized constructor
        final CommentDto.ExerciseField exerciseField2 = new CommentDto.ExerciseField(1L);
        assertEquals(1L, exerciseField2.id);

        // Test setting fields
        exerciseField.id = 2L;
        assertEquals(2L, exerciseField.id);
    }

    @Test
    @DisplayName("Should handle null values in sync")
    void shouldHandleNullValuesInSync() {
        // Given - all null
        this.commentDto.exerciseId = null;
        this.commentDto.exercise = null;

        // When
        this.commentDto.syncExercise();

        // Then - should remain null
        assertNull(this.commentDto.exerciseId);
        assertNull(this.commentDto.exercise);
    }

    @Test
    @DisplayName("Should handle long content")
    void shouldHandleLongContent() {
        // Given
        final StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            longContent.append("This is a very long comment. ");
        }

        // When
        this.commentDto.content = longContent.toString();

        // Then
        assertEquals(longContent.toString(), this.commentDto.content);
        assertTrue(this.commentDto.content.length() > 10000);
    }

    @Test
    @DisplayName("Should handle empty content")
    void shouldHandleEmptyContent() {
        // When
        this.commentDto.content = "";

        // Then
        assertEquals("", this.commentDto.content);
    }

    @Test
    @DisplayName("Should handle null content")
    void shouldHandleNullContent() {
        // When
        this.commentDto.content = null;

        // Then
        assertNull(this.commentDto.content);
    }

    @Test
    @DisplayName("Should handle special characters in content")
    void shouldHandleSpecialCharactersInContent() {
        // Given
        final String specialContent = "Comment with special chars: @#$%^&*()_+-={}[]|\\:;\"'<>,.?/~`\nNew line\tTab";

        // When
        this.commentDto.content = specialContent;

        // Then
        assertEquals(specialContent, this.commentDto.content);
    }

    @Test
    @DisplayName("Should handle HTML content")
    void shouldHandleHtmlContent() {
        // Given
        final String htmlContent = "<p>This is <strong>bold</strong> and <em>italic</em> text with <a href=\"#\">links</a>.</p>";

        // When
        this.commentDto.content = htmlContent;

        // Then
        assertEquals(htmlContent, this.commentDto.content);
    }

    @Test
    @DisplayName("Should handle markdown content")
    void shouldHandleMarkdownContent() {
        // Given
        final String markdownContent = "This is **bold** and *italic* text with [links](http://example.com).\n\n- List item 1\n- List item 2";

        // When
        this.commentDto.content = markdownContent;

        // Then
        assertEquals(markdownContent, this.commentDto.content);
    }

    @Test
    @DisplayName("Should handle exercise field with null id")
    void shouldHandleExerciseFieldWithNullId() {
        // Given
        final CommentDto.ExerciseField exerciseField = new CommentDto.ExerciseField(null);
        this.commentDto.exercise = exerciseField;

        // When
        this.commentDto.syncExercise();

        // Then - exercise field exists but has null id, so exerciseId should remain
        // null
        assertNotNull(this.commentDto.exercise);
        assertNull(this.commentDto.exercise.id);
        assertNull(this.commentDto.exerciseId);
    }

    @Test
    @DisplayName("Should handle content with quotes and escapes")
    void shouldHandleContentWithQuotesAndEscapes() {
        // Given
        final String contentWithQuotes = "He said \"Hello World\" and she replied 'Hi there!'";
        final String contentWithEscapes = "Path: C:\\Users\\test\\file.txt";
        final String contentWithJson = "{\"message\": \"Hello\", \"status\": \"success\"}";

        // When & Then
        this.commentDto.content = contentWithQuotes;
        assertEquals(contentWithQuotes, this.commentDto.content);

        this.commentDto.content = contentWithEscapes;
        assertEquals(contentWithEscapes, this.commentDto.content);

        this.commentDto.content = contentWithJson;
        assertEquals(contentWithJson, this.commentDto.content);
    }

    @Test
    @DisplayName("Should handle emoji and unicode content")
    void shouldHandleEmojiAndUnicodeContent() {
        // Given
        final String emojiContent = "Great exercise! 👍 😊 🚀 ❤️";
        final String unicodeContent = "Café, résumé, naïve, Zürich, 北京";

        // When & Then
        this.commentDto.content = emojiContent;
        assertEquals(emojiContent, this.commentDto.content);

        this.commentDto.content = unicodeContent;
        assertEquals(unicodeContent, this.commentDto.content);
    }
}