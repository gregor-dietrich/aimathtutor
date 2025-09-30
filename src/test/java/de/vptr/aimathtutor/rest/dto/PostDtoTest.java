package de.vptr.aimathtutor.rest.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PostDtoTest {

    private PostDto postDto;

    @BeforeEach
    void setUp() {
        postDto = new PostDto();
    }

    @Test
    @DisplayName("Should create PostDto with default constructor")
    void shouldCreatePostDtoWithDefaultConstructor() {
        // Then
        assertNull(postDto.id);
        assertNull(postDto.title);
        assertNull(postDto.content);
        assertNull(postDto.userId);
        assertNull(postDto.categoryId);
        assertNull(postDto.published);
        assertNull(postDto.commentable);
        assertNull(postDto.created);
        assertNull(postDto.lastEdit);
        assertNull(postDto.user);
        assertNull(postDto.category);
    }

    @Test
    @DisplayName("Should create PostDto with parameterized constructor")
    void shouldCreatePostDtoWithParameterizedConstructor() {
        // When
        PostDto dto = new PostDto("Test Title", "Test Content", 1L, 2L, true, false);

        // Then
        assertEquals("Test Title", dto.title);
        assertEquals("Test Content", dto.content);
        assertEquals(1L, dto.userId);
        assertEquals(2L, dto.categoryId);
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
        LocalDateTime now = LocalDateTime.now();

        // When
        postDto.id = 1L;
        postDto.title = "Test Post";
        postDto.content = "This is test content";
        postDto.userId = 2L;
        postDto.categoryId = 3L;
        postDto.published = true;
        postDto.commentable = false;
        postDto.created = now;
        postDto.lastEdit = now;

        // Then
        assertEquals(1L, postDto.id);
        assertEquals("Test Post", postDto.title);
        assertEquals("This is test content", postDto.content);
        assertEquals(2L, postDto.userId);
        assertEquals(3L, postDto.categoryId);
        assertTrue(postDto.published);
        assertFalse(postDto.commentable);
        assertEquals(now, postDto.created);
        assertEquals(now, postDto.lastEdit);
    }

    @Test
    @DisplayName("Should sync nested user field correctly")
    void shouldSyncNestedUserFieldCorrectly() {
        // Given
        postDto.userId = 1L;

        // When
        postDto.syncNestedFields();

        // Then
        assertNotNull(postDto.user);
        assertEquals(1L, postDto.user.id);

        // Given - Set user object
        PostDto.UserField userField = new PostDto.UserField(2L);
        userField.username = "testuser";
        postDto.user = userField;
        postDto.userId = null;

        // When
        postDto.syncNestedFields();

        // Then
        assertEquals(2L, postDto.userId);
    }

    @Test
    @DisplayName("Should sync nested category field correctly")
    void shouldSyncNestedCategoryFieldCorrectly() {
        // Given
        postDto.categoryId = 1L;

        // When
        postDto.syncNestedFields();

        // Then
        assertNotNull(postDto.category);
        assertEquals(1L, postDto.category.id);

        // Given - Set category object
        PostDto.CategoryField categoryField = new PostDto.CategoryField(2L);
        categoryField.name = "Test Category";
        postDto.category = categoryField;
        postDto.categoryId = null;

        // When
        postDto.syncNestedFields();

        // Then
        assertEquals(2L, postDto.categoryId);
    }

    @Test
    @DisplayName("Should handle UserField operations")
    void shouldHandleUserFieldOperations() {
        // Test default constructor
        PostDto.UserField userField = new PostDto.UserField();
        assertNull(userField.id);
        assertNull(userField.username);

        // Test parameterized constructor
        PostDto.UserField userField2 = new PostDto.UserField(1L);
        assertEquals(1L, userField2.id);
        assertNull(userField2.username);

        // Test setters
        userField.setId(2L);
        userField.setUsername("testuser");
        assertEquals(2L, userField.id);
        assertEquals("testuser", userField.username);
    }

    @Test
    @DisplayName("Should handle CategoryField operations")
    void shouldHandleCategoryFieldOperations() {
        // Test default constructor
        PostDto.CategoryField categoryField = new PostDto.CategoryField();
        assertNull(categoryField.id);
        assertNull(categoryField.name);

        // Test parameterized constructor
        PostDto.CategoryField categoryField2 = new PostDto.CategoryField(1L);
        assertEquals(1L, categoryField2.id);
        assertNull(categoryField2.name);

        // Test setting fields
        categoryField.id = 2L;
        categoryField.name = "Test Category";
        assertEquals(2L, categoryField.id);
        assertEquals("Test Category", categoryField.name);
    }

    @Test
    @DisplayName("Should handle null values in sync")
    void shouldHandleNullValuesInSync() {
        // Given - all null
        postDto.userId = null;
        postDto.categoryId = null;
        postDto.user = null;
        postDto.category = null;

        // When
        postDto.syncNestedFields();

        // Then - should remain null
        assertNull(postDto.userId);
        assertNull(postDto.categoryId);
        assertNull(postDto.user);
        assertNull(postDto.category);
    }

    @Test
    @DisplayName("Should handle long content")
    void shouldHandleLongContent() {
        // Given
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longContent.append("This is a very long post content. ");
        }

        // When
        postDto.content = longContent.toString();

        // Then
        assertEquals(longContent.toString(), postDto.content);
        assertTrue(postDto.content.length() > 10000);
    }

    @Test
    @DisplayName("Should handle special characters in content")
    void shouldHandleSpecialCharactersInContent() {
        // Given
        String specialContent = "Content with special chars: @#$%^&*()_+-={}[]|\\:;\"'<>,.?/~`\nNew line\tTab";

        // When
        postDto.title = "Title with émojis 🚀 and ünicöde";
        postDto.content = specialContent;

        // Then
        assertEquals("Title with émojis 🚀 and ünicöde", postDto.title);
        assertEquals(specialContent, postDto.content);
    }

    @Test
    @DisplayName("Should handle HTML content")
    void shouldHandleHtmlContent() {
        // Given
        String htmlContent = "<h1>Title</h1><p>Paragraph with <strong>bold</strong> and <em>italic</em> text.</p><ul><li>Item 1</li><li>Item 2</li></ul>";

        // When
        postDto.content = htmlContent;

        // Then
        assertEquals(htmlContent, postDto.content);
    }

    @Test
    @DisplayName("Should handle markdown content")
    void shouldHandleMarkdownContent() {
        // Given
        String markdownContent = "# Title\n\nThis is a paragraph with **bold** and *italic* text.\n\n- Item 1\n- Item 2\n\n```java\nSystem.out.println(\"Hello World\");\n```";

        // When
        postDto.content = markdownContent;

        // Then
        assertEquals(markdownContent, postDto.content);
    }
}