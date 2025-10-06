package de.vptr.aimathtutor.rest.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.rest.entity.CommentEntity;
import de.vptr.aimathtutor.rest.entity.ExerciseEntity;
import de.vptr.aimathtutor.rest.entity.UserEntity;

class CommentViewDtoTest {

    @Test
    @DisplayName("Should create CommentViewDto from CommentEntity")
    void shouldCreateCommentViewDtoFromCommentEntity() {
        // Given
        final CommentEntity commentEntity = new CommentEntity();
        commentEntity.id = 1L;
        commentEntity.content = "Test comment";
        commentEntity.created = LocalDateTime.now();

        final ExerciseEntity exerciseEntity = new ExerciseEntity();
        exerciseEntity.id = 100L;
        exerciseEntity.title = "Test Exercise";
        commentEntity.exercise = exerciseEntity;

        final UserEntity userEntity = new UserEntity();
        userEntity.id = 50L;
        userEntity.username = "testuser";
        commentEntity.user = userEntity;

        // When
        final CommentViewDto dto = new CommentViewDto(commentEntity);

        // Then
        assertEquals(1L, dto.id);
        assertEquals("Test comment", dto.content);
        assertEquals(commentEntity.created, dto.created);
        assertEquals(100L, dto.exerciseId);
        assertEquals("Test Exercise", dto.exerciseTitle);
        assertEquals(50L, dto.userId);
        assertEquals("testuser", dto.username);
    }

    @Test
    @DisplayName("Should handle null exercise in CommentEntity")
    void shouldHandleNullExerciseInCommentEntity() {
        // Given
        final CommentEntity commentEntity = new CommentEntity();
        commentEntity.id = 1L;
        commentEntity.content = "Test comment";
        commentEntity.created = LocalDateTime.now();
        commentEntity.exercise = null;

        final UserEntity userEntity = new UserEntity();
        userEntity.id = 50L;
        userEntity.username = "testuser";
        commentEntity.user = userEntity;

        // When
        final CommentViewDto dto = new CommentViewDto(commentEntity);

        // Then
        assertEquals(1L, dto.id);
        assertEquals("Test comment", dto.content);
        assertNull(dto.exerciseId);
        assertNull(dto.exerciseTitle);
        assertEquals(50L, dto.userId);
        assertEquals("testuser", dto.username);
    }

    @Test
    @DisplayName("Should handle null user in CommentEntity")
    void shouldHandleNullUserInCommentEntity() {
        // Given
        final CommentEntity commentEntity = new CommentEntity();
        commentEntity.id = 1L;
        commentEntity.content = "Test comment";
        commentEntity.created = LocalDateTime.now();

        final ExerciseEntity exerciseEntity = new ExerciseEntity();
        exerciseEntity.id = 100L;
        exerciseEntity.title = "Test Exercise";
        commentEntity.exercise = exerciseEntity;
        commentEntity.user = null;

        // When
        final CommentViewDto dto = new CommentViewDto(commentEntity);

        // Then
        assertEquals(1L, dto.id);
        assertEquals("Test comment", dto.content);
        assertEquals(100L, dto.exerciseId);
        assertEquals("Test Exercise", dto.exerciseTitle);
        assertNull(dto.userId);
        assertNull(dto.username);
    }
}
