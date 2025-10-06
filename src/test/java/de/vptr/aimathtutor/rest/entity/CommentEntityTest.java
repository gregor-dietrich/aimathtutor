package de.vptr.aimathtutor.rest.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommentEntityTest {

    private CommentEntity comment;
    private ExerciseEntity exercise;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        this.comment = new CommentEntity();

        this.exercise = new ExerciseEntity();
        this.exercise.id = 1L;
        this.exercise.title = "Test Exercise";

        this.user = new UserEntity();
        this.user.id = 1L;
        this.user.username = "testuser";
    }

    @Test
    @DisplayName("Should create CommentEntity with all fields")
    void shouldCreateCommentEntityWithAllFields() {
        // Given
        final LocalDateTime now = LocalDateTime.now();

        // When
        this.comment.id = 1L;
        this.comment.content = "This is a test comment";
        this.comment.exercise = this.exercise;
        this.comment.user = this.user;
        this.comment.created = now;

        // Then
        assertEquals(1L, this.comment.id);
        assertEquals("This is a test comment", this.comment.content);
        assertEquals(this.exercise, this.comment.exercise);
        assertEquals(this.user, this.comment.user);
        assertEquals(now, this.comment.created);
    }

    @Test
    @DisplayName("Should handle null values for optional fields")
    void shouldHandleNullValuesForOptionalFields() {
        // When
        this.comment.id = 1L;
        this.comment.content = "Required content";
        this.comment.exercise = this.exercise; // Required field
        this.comment.user = null; // Optional field
        this.comment.created = null;

        // Then
        assertNotNull(this.comment.exercise);
        assertNull(this.comment.user);
        assertNull(this.comment.created);
    }

    @Test
    @DisplayName("Should handle anonymous comments")
    void shouldHandleAnonymousComments() {
        // Given
        final LocalDateTime now = LocalDateTime.now();

        // When
        this.comment.id = 1L;
        this.comment.content = "Anonymous comment";
        this.comment.exercise = this.exercise;
        this.comment.user = null; // Anonymous comment
        this.comment.created = now;

        // Then
        assertEquals("Anonymous comment", this.comment.content);
        assertEquals(this.exercise, this.comment.exercise);
        assertNull(this.comment.user);
        assertEquals(now, this.comment.created);
    }

    @Test
    @DisplayName("Should maintain relationship with exercise")
    void shouldMaintainRelationshipWithExercise() {
        // When
        this.comment.exercise = this.exercise;

        // Then
        assertNotNull(this.comment.exercise);
        assertEquals(1L, this.comment.exercise.id);
        assertEquals("Test Exercise", this.comment.exercise.title);
    }

    @Test
    @DisplayName("Should maintain relationship with user")
    void shouldMaintainRelationshipWithUser() {
        // When
        this.comment.user = this.user;

        // Then
        assertNotNull(this.comment.user);
        assertEquals(1L, this.comment.user.id);
        assertEquals("testuser", this.comment.user.username);
    }

    @Test
    @DisplayName("Should handle long content")
    void shouldHandleLongContent() {
        // Given
        final StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longContent.append("This is a very long comment content. ");
        }

        // When
        this.comment.content = longContent.toString();

        // Then
        assertEquals(longContent.toString(), this.comment.content);
        assertTrue(this.comment.content.length() > 1000);
    }

    @Test
    @DisplayName("Should handle empty content")
    void shouldHandleEmptyContent() {
        // When
        this.comment.content = "";

        // Then
        assertEquals("", this.comment.content);
    }

    @Test
    @DisplayName("Should handle content with special characters")
    void shouldHandleContentWithSpecialCharacters() {
        // Given
        final String specialContent = "Comment with special chars: @#$%^&*()_+-={}[]|\\:;\"'<>,.?/~`";

        // When
        this.comment.content = specialContent;

        // Then
        assertEquals(specialContent, this.comment.content);
    }

    @Test
    @DisplayName("Should handle content with newlines and formatting")
    void shouldHandleContentWithNewlinesAndFormatting() {
        // Given
        final String formattedContent = "Line 1\nLine 2\n\tTabbed line\n    Spaced line";

        // When
        this.comment.content = formattedContent;

        // Then
        assertEquals(formattedContent, this.comment.content);
        assertTrue(this.comment.content.contains("\n"));
        assertTrue(this.comment.content.contains("\t"));
    }
}