package de.vptr.aimathtutor.rest.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PostEntityTest {

    private PostEntity postEntity;
    private UserEntity user;
    private PostCategoryEntity category;

    @BeforeEach
    void setUp() {
        postEntity = new PostEntity();
        user = new UserEntity();
        user.id = 1L;
        user.username = "testuser";

        category = new PostCategoryEntity();
        category.id = 1L;
        category.name = "General";
    }

    @Test
    @DisplayName("Should create PostEntity with all fields")
    void shouldCreatePostEntityWithAllFields() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        List<PostCommentEntity> comments = new ArrayList<>();

        // When
        postEntity.id = 1L;
        postEntity.title = "Test Post";
        postEntity.content = "This is test content";
        postEntity.user = user;
        postEntity.category = category;
        postEntity.published = true;
        postEntity.commentable = true;
        postEntity.created = now;
        postEntity.lastEdit = now;
        postEntity.comments = comments;

        // Then
        assertEquals(1L, postEntity.id);
        assertEquals("Test Post", postEntity.title);
        assertEquals("This is test content", postEntity.content);
        assertEquals(user, postEntity.user);
        assertEquals(category, postEntity.category);
        assertTrue(postEntity.published);
        assertTrue(postEntity.commentable);
        assertEquals(now, postEntity.created);
        assertEquals(now, postEntity.lastEdit);
        assertEquals(comments, postEntity.comments);
    }

    @Test
    @DisplayName("Should have default values for boolean fields")
    void shouldHaveDefaultValuesForBooleanFields() {
        // Then
        assertFalse(postEntity.published);
        assertFalse(postEntity.commentable);
    }

    @Test
    @DisplayName("Should handle null values for optional fields")
    void shouldHandleNullValuesForOptionalFields() {
        // When
        postEntity.title = "Required Title";
        postEntity.content = "Required Content";
        postEntity.user = null;
        postEntity.category = null;
        postEntity.created = null;
        postEntity.lastEdit = null;
        postEntity.comments = null;

        // Then
        assertNull(postEntity.user);
        assertNull(postEntity.category);
        assertNull(postEntity.created);
        assertNull(postEntity.lastEdit);
        assertNull(postEntity.comments);
    }

    @Test
    @DisplayName("Should handle comments collection properly")
    void shouldHandleCommentsCollectionProperly() {
        // Given
        List<PostCommentEntity> comments = new ArrayList<>();
        PostCommentEntity comment1 = new PostCommentEntity();
        comment1.id = 1L;
        comment1.content = "First comment";

        PostCommentEntity comment2 = new PostCommentEntity();
        comment2.id = 2L;
        comment2.content = "Second comment";

        comments.add(comment1);
        comments.add(comment2);

        // When
        postEntity.comments = comments;

        // Then
        assertNotNull(postEntity.comments);
        assertEquals(2, postEntity.comments.size());
        assertEquals(comment1, postEntity.comments.get(0));
        assertEquals(comment2, postEntity.comments.get(1));
    }

    @Test
    @DisplayName("Should handle empty comments collection")
    void shouldHandleEmptyCommentsCollection() {
        // When
        postEntity.comments = new ArrayList<>();

        // Then
        assertNotNull(postEntity.comments);
        assertTrue(postEntity.comments.isEmpty());
    }

    @Test
    @DisplayName("Should set boolean fields correctly")
    void shouldSetBooleanFieldsCorrectly() {
        // When
        postEntity.published = true;
        postEntity.commentable = false;

        // Then
        assertTrue(postEntity.published);
        assertFalse(postEntity.commentable);
    }
}