package de.vptr.aimathtutor.rest.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.rest.entity.PostCommentEntity;
import de.vptr.aimathtutor.rest.entity.PostEntity;
import de.vptr.aimathtutor.rest.entity.UserEntity;

class PostCommentViewDtoTest {

    @Test
    @DisplayName("Should create PostCommentViewDto from PostCommentEntity")
    void shouldCreatePostCommentViewDtoFromPostCommentEntity() {
        // Given
        PostCommentEntity commentEntity = new PostCommentEntity();
        commentEntity.id = 1L;
        commentEntity.content = "Test comment";
        commentEntity.created = LocalDateTime.now();

        PostEntity postEntity = new PostEntity();
        postEntity.id = 100L;
        postEntity.title = "Test Post";
        commentEntity.post = postEntity;

        UserEntity userEntity = new UserEntity();
        userEntity.id = 50L;
        userEntity.username = "testuser";
        commentEntity.user = userEntity;

        // When
        PostCommentViewDto dto = new PostCommentViewDto(commentEntity);

        // Then
        assertEquals(1L, dto.id);
        assertEquals("Test comment", dto.content);
        assertEquals(commentEntity.created, dto.created);
        assertEquals(100L, dto.postId);
        assertEquals("Test Post", dto.postTitle);
        assertEquals(50L, dto.userId);
        assertEquals("testuser", dto.username);
    }

    @Test
    @DisplayName("Should handle null post in PostCommentEntity")
    void shouldHandleNullPostInPostCommentEntity() {
        // Given
        PostCommentEntity commentEntity = new PostCommentEntity();
        commentEntity.id = 1L;
        commentEntity.content = "Test comment";
        commentEntity.created = LocalDateTime.now();
        commentEntity.post = null;

        UserEntity userEntity = new UserEntity();
        userEntity.id = 50L;
        userEntity.username = "testuser";
        commentEntity.user = userEntity;

        // When
        PostCommentViewDto dto = new PostCommentViewDto(commentEntity);

        // Then
        assertEquals(1L, dto.id);
        assertEquals("Test comment", dto.content);
        assertNull(dto.postId);
        assertNull(dto.postTitle);
        assertEquals(50L, dto.userId);
        assertEquals("testuser", dto.username);
    }

    @Test
    @DisplayName("Should handle null user in PostCommentEntity")
    void shouldHandleNullUserInPostCommentEntity() {
        // Given
        PostCommentEntity commentEntity = new PostCommentEntity();
        commentEntity.id = 1L;
        commentEntity.content = "Test comment";
        commentEntity.created = LocalDateTime.now();

        PostEntity postEntity = new PostEntity();
        postEntity.id = 100L;
        postEntity.title = "Test Post";
        commentEntity.post = postEntity;
        commentEntity.user = null;

        // When
        PostCommentViewDto dto = new PostCommentViewDto(commentEntity);

        // Then
        assertEquals(1L, dto.id);
        assertEquals("Test comment", dto.content);
        assertEquals(100L, dto.postId);
        assertEquals("Test Post", dto.postTitle);
        assertNull(dto.userId);
        assertNull(dto.username);
    }
}
