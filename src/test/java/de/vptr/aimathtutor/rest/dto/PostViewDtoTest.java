package de.vptr.aimathtutor.rest.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.rest.entity.PostCategoryEntity;
import de.vptr.aimathtutor.rest.entity.PostEntity;
import de.vptr.aimathtutor.rest.entity.UserEntity;

class PostViewDtoTest {

    private PostEntity postEntity;
    private UserEntity userEntity;
    private PostCategoryEntity categoryEntity;

    @BeforeEach
    void setUp() {
        userEntity = new UserEntity();
        userEntity.id = 1L;
        userEntity.username = "testuser";

        categoryEntity = new PostCategoryEntity();
        categoryEntity.id = 5L;
        categoryEntity.name = "Test Category";

        postEntity = new PostEntity();
        postEntity.id = 10L;
        postEntity.title = "Test Post";
        postEntity.content = "Test post content";
        postEntity.published = true;
        postEntity.commentable = true;
        postEntity.created = LocalDateTime.of(2025, 10, 1, 12, 0);
        postEntity.lastEdit = LocalDateTime.of(2025, 10, 1, 13, 0);
        postEntity.user = userEntity;
        postEntity.category = categoryEntity;
    }

    @Test
    @DisplayName("Should create DTO from entity")
    void shouldCreateDtoFromEntity() {
        PostViewDto dto = new PostViewDto(postEntity);

        assertEquals(10L, dto.id);
        assertEquals("Test Post", dto.title);
        assertEquals("Test post content", dto.content);
        assertTrue(dto.published);
        assertTrue(dto.commentable);
        assertEquals(LocalDateTime.of(2025, 10, 1, 12, 0), dto.created);
        assertEquals(LocalDateTime.of(2025, 10, 1, 13, 0), dto.lastEdit);
        assertEquals(1L, dto.userId);
        assertEquals("testuser", dto.username);
        assertEquals(5L, dto.categoryId);
        assertEquals("Test Category", dto.categoryName);
    }

    @Test
    @DisplayName("Should handle null user")
    void shouldHandleNullUser() {
        postEntity.user = null;
        PostViewDto dto = new PostViewDto(postEntity);

        assertNull(dto.userId);
        assertNull(dto.username);
    }

    @Test
    @DisplayName("Should handle null category")
    void shouldHandleNullCategory() {
        postEntity.category = null;
        PostViewDto dto = new PostViewDto(postEntity);

        assertNull(dto.categoryId);
        assertNull(dto.categoryName);
    }

    @Test
    @DisplayName("Should handle unpublished post")
    void shouldHandleUnpublishedPost() {
        postEntity.published = false;
        PostViewDto dto = new PostViewDto(postEntity);

        assertFalse(dto.published);
    }

    @Test
    @DisplayName("Should handle non-commentable post")
    void shouldHandleNonCommentablePost() {
        postEntity.commentable = false;
        PostViewDto dto = new PostViewDto(postEntity);

        assertFalse(dto.commentable);
    }
}
