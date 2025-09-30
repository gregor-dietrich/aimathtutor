package de.vptr.aimathtutor.rest.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PostCommentDtoTest {

    private PostCommentDto commentDto;

    @BeforeEach
    void setUp() {
        commentDto = new PostCommentDto();
    }

    @Test
    @DisplayName("Should create PostCommentDto with default constructor")
    void shouldCreatePostCommentDtoWithDefaultConstructor() {
        // Then
        assertNull(commentDto.id);
        assertNull(commentDto.content);
        assertNull(commentDto.postId);
        assertNull(commentDto.post);
    }

    @Test
    @DisplayName("Should set all fields correctly")
    void shouldSetAllFieldsCorrectly() {
        // When
        commentDto.id = 1L;
        commentDto.content = "This is a test comment";
        commentDto.postId = 2L;

        // Then
        assertEquals(1L, commentDto.id);
        assertEquals("This is a test comment", commentDto.content);
        assertEquals(2L, commentDto.postId);
    }

    @Test
    @DisplayName("Should sync post field correctly")
    void shouldSyncPostFieldCorrectly() {
        // Given
        commentDto.postId = 1L;

        // When
        commentDto.syncPost();

        // Then
        assertNotNull(commentDto.post);
        assertEquals(1L, commentDto.post.id);

        // Given - Set post object
        PostCommentDto.PostField postField = new PostCommentDto.PostField(2L);
        commentDto.post = postField;
        commentDto.postId = null;

        // When
        commentDto.syncPost();

        // Then
        assertEquals(2L, commentDto.postId);
    }

    @Test
    @DisplayName("Should handle PostField operations")
    void shouldHandlePostFieldOperations() {
        // Test default constructor
        PostCommentDto.PostField postField = new PostCommentDto.PostField();
        assertNull(postField.id);

        // Test parameterized constructor
        PostCommentDto.PostField postField2 = new PostCommentDto.PostField(1L);
        assertEquals(1L, postField2.id);

        // Test setting fields
        postField.id = 2L;
        assertEquals(2L, postField.id);
    }

    @Test
    @DisplayName("Should handle null values in sync")
    void shouldHandleNullValuesInSync() {
        // Given - all null
        commentDto.postId = null;
        commentDto.post = null;

        // When
        commentDto.syncPost();

        // Then - should remain null
        assertNull(commentDto.postId);
        assertNull(commentDto.post);
    }

    @Test
    @DisplayName("Should handle long content")
    void shouldHandleLongContent() {
        // Given
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            longContent.append("This is a very long comment. ");
        }

        // When
        commentDto.content = longContent.toString();

        // Then
        assertEquals(longContent.toString(), commentDto.content);
        assertTrue(commentDto.content.length() > 10000);
    }

    @Test
    @DisplayName("Should handle empty content")
    void shouldHandleEmptyContent() {
        // When
        commentDto.content = "";

        // Then
        assertEquals("", commentDto.content);
    }

    @Test
    @DisplayName("Should handle null content")
    void shouldHandleNullContent() {
        // When
        commentDto.content = null;

        // Then
        assertNull(commentDto.content);
    }

    @Test
    @DisplayName("Should handle special characters in content")
    void shouldHandleSpecialCharactersInContent() {
        // Given
        String specialContent = "Comment with special chars: @#$%^&*()_+-={}[]|\\:;\"'<>,.?/~`\nNew line\tTab";

        // When
        commentDto.content = specialContent;

        // Then
        assertEquals(specialContent, commentDto.content);
    }

    @Test
    @DisplayName("Should handle HTML content")
    void shouldHandleHtmlContent() {
        // Given
        String htmlContent = "<p>This is <strong>bold</strong> and <em>italic</em> text with <a href=\"#\">links</a>.</p>";

        // When
        commentDto.content = htmlContent;

        // Then
        assertEquals(htmlContent, commentDto.content);
    }

    @Test
    @DisplayName("Should handle markdown content")
    void shouldHandleMarkdownContent() {
        // Given
        String markdownContent = "This is **bold** and *italic* text with [links](http://example.com).\n\n- List item 1\n- List item 2";

        // When
        commentDto.content = markdownContent;

        // Then
        assertEquals(markdownContent, commentDto.content);
    }

    @Test
    @DisplayName("Should handle post field with null id")
    void shouldHandlePostFieldWithNullId() {
        // Given
        PostCommentDto.PostField postField = new PostCommentDto.PostField(null);
        commentDto.post = postField;

        // When
        commentDto.syncPost();

        // Then - post field exists but has null id, so postId should remain null
        assertNotNull(commentDto.post);
        assertNull(commentDto.post.id);
        assertNull(commentDto.postId);
    }

    @Test
    @DisplayName("Should handle content with quotes and escapes")
    void shouldHandleContentWithQuotesAndEscapes() {
        // Given
        String contentWithQuotes = "He said \"Hello World\" and she replied 'Hi there!'";
        String contentWithEscapes = "Path: C:\\Users\\test\\file.txt";
        String contentWithJson = "{\"message\": \"Hello\", \"status\": \"success\"}";

        // When & Then
        commentDto.content = contentWithQuotes;
        assertEquals(contentWithQuotes, commentDto.content);

        commentDto.content = contentWithEscapes;
        assertEquals(contentWithEscapes, commentDto.content);

        commentDto.content = contentWithJson;
        assertEquals(contentWithJson, commentDto.content);
    }

    @Test
    @DisplayName("Should handle emoji and unicode content")
    void shouldHandleEmojiAndUnicodeContent() {
        // Given
        String emojiContent = "Great post! 👍 😊 🚀 ❤️";
        String unicodeContent = "Café, résumé, naïve, Zürich, 北京";

        // When & Then
        commentDto.content = emojiContent;
        assertEquals(emojiContent, commentDto.content);

        commentDto.content = unicodeContent;
        assertEquals(unicodeContent, commentDto.content);
    }
}