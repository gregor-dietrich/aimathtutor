package de.vptr.aimathtutor.rest.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PostCommentEntityTest {

    private PostCommentEntity comment;
    private PostEntity post;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        comment = new PostCommentEntity();

        post = new PostEntity();
        post.id = 1L;
        post.title = "Test Post";

        user = new UserEntity();
        user.id = 1L;
        user.username = "testuser";
    }

    @Test
    @DisplayName("Should create PostCommentEntity with all fields")
    void shouldCreatePostCommentEntityWithAllFields() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // When
        comment.id = 1L;
        comment.content = "This is a test comment";
        comment.post = post;
        comment.user = user;
        comment.created = now;

        // Then
        assertEquals(1L, comment.id);
        assertEquals("This is a test comment", comment.content);
        assertEquals(post, comment.post);
        assertEquals(user, comment.user);
        assertEquals(now, comment.created);
    }

    @Test
    @DisplayName("Should handle null values for optional fields")
    void shouldHandleNullValuesForOptionalFields() {
        // When
        comment.id = 1L;
        comment.content = "Required content";
        comment.post = post; // Required field
        comment.user = null; // Optional field
        comment.created = null;

        // Then
        assertNotNull(comment.post);
        assertNull(comment.user);
        assertNull(comment.created);
    }

    @Test
    @DisplayName("Should handle anonymous comments")
    void shouldHandleAnonymousComments() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // When
        comment.id = 1L;
        comment.content = "Anonymous comment";
        comment.post = post;
        comment.user = null; // Anonymous comment
        comment.created = now;

        // Then
        assertEquals("Anonymous comment", comment.content);
        assertEquals(post, comment.post);
        assertNull(comment.user);
        assertEquals(now, comment.created);
    }

    @Test
    @DisplayName("Should maintain relationship with post")
    void shouldMaintainRelationshipWithPost() {
        // When
        comment.post = post;

        // Then
        assertNotNull(comment.post);
        assertEquals(1L, comment.post.id);
        assertEquals("Test Post", comment.post.title);
    }

    @Test
    @DisplayName("Should maintain relationship with user")
    void shouldMaintainRelationshipWithUser() {
        // When
        comment.user = user;

        // Then
        assertNotNull(comment.user);
        assertEquals(1L, comment.user.id);
        assertEquals("testuser", comment.user.username);
    }

    @Test
    @DisplayName("Should handle long content")
    void shouldHandleLongContent() {
        // Given
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longContent.append("This is a very long comment content. ");
        }

        // When
        comment.content = longContent.toString();

        // Then
        assertEquals(longContent.toString(), comment.content);
        assertTrue(comment.content.length() > 1000);
    }

    @Test
    @DisplayName("Should handle empty content")
    void shouldHandleEmptyContent() {
        // When
        comment.content = "";

        // Then
        assertEquals("", comment.content);
    }

    @Test
    @DisplayName("Should handle content with special characters")
    void shouldHandleContentWithSpecialCharacters() {
        // Given
        String specialContent = "Comment with special chars: @#$%^&*()_+-={}[]|\\:;\"'<>,.?/~`";

        // When
        comment.content = specialContent;

        // Then
        assertEquals(specialContent, comment.content);
    }

    @Test
    @DisplayName("Should handle content with newlines and formatting")
    void shouldHandleContentWithNewlinesAndFormatting() {
        // Given
        String formattedContent = "Line 1\nLine 2\n\tTabbed line\n    Spaced line";

        // When
        comment.content = formattedContent;

        // Then
        assertEquals(formattedContent, comment.content);
        assertTrue(comment.content.contains("\n"));
        assertTrue(comment.content.contains("\t"));
    }
}