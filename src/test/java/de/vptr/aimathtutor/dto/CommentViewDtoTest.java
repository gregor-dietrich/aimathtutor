package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.CommentEntity;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.UserEntity;

@SuppressWarnings("NullAway")
class CommentViewDtoTest {

    @Test
    @DisplayName("Default constructor has null fields")
    void testDefaultConstructor() {
        final var dto = new CommentViewDto();
        assertNull(dto.publicId);
        assertNull(dto.content);
        assertNull(dto.exercisePublicId);
        assertNull(dto.username);
    }

    @Test
    @DisplayName("Constructor from entity with user and exercise maps all fields")
    void testConstructorFromEntity() {
        final var user = new UserEntity();
        user.id = 1L;
        user.publicId = "user-1";
        user.username = "testuser";

        final var exercise = new ExerciseEntity();
        exercise.id = 2L;
        exercise.publicId = "ex-1";
        exercise.title = "Test Exercise";

        final var entity = new CommentEntity();
        entity.publicId = "comment-1";
        entity.content = "Great explanation!";
        entity.status = CommentDto.CommentStatus.VISIBLE;
        entity.flagsCount = 3;
        entity.sessionId = "session-abc";
        entity.user = user;
        entity.exercise = exercise;

        final var dto = new CommentViewDto(entity);
        assertEquals("comment-1", dto.publicId);
        assertEquals("Great explanation!", dto.content);
        assertEquals("user-1", dto.userPublicId);
        assertEquals("testuser", dto.username);
        assertEquals("user-1", dto.authorPublicId);
        assertEquals(1L, dto.userId);
        assertEquals(1L, dto.authorId);
        assertEquals("ex-1", dto.exercisePublicId);
        assertEquals("Test Exercise", dto.exerciseTitle);
        assertEquals(2L, dto.exerciseId);
        assertEquals(CommentDto.CommentStatus.VISIBLE, dto.status);
        assertEquals(3, dto.flagsCount);
        assertEquals("session-abc", dto.sessionId);
    }

    @Test
    @DisplayName("Constructor from entity with null user and exercise")
    void testConstructorFromEntityNullRelations() {
        final var entity = new CommentEntity();
        entity.publicId = "comment-2";
        entity.content = "Test";

        final var dto = new CommentViewDto(entity);
        assertNull(dto.userPublicId);
        assertNull(dto.username);
        assertNull(dto.exercisePublicId);
        assertNull(dto.exerciseTitle);
    }

    @Test
    @DisplayName("Constructor from entity with parent comment maps parent fields")
    void testConstructorFromEntityWithParent() {
        final var parent = new CommentEntity();
        parent.publicId = "parent-1";
        parent.id = 10L;

        final var entity = new CommentEntity();
        entity.publicId = "reply-1";
        entity.content = "Reply";
        entity.parentComment = parent;

        final var dto = new CommentViewDto(entity);
        assertEquals("parent-1", dto.parentPublicId);
        assertEquals(10L, dto.parentId);
    }

    @Test
    @DisplayName("Constructor from entity defaults status to VISIBLE when null")
    void testConstructorFromEntityNullStatus() {
        final var entity = new CommentEntity();
        entity.publicId = "c-1";
        entity.content = "No status";
        entity.status = null;

        final var dto = new CommentViewDto(entity);
        assertEquals(CommentDto.CommentStatus.VISIBLE, dto.status);
    }

    @Test
    @DisplayName("Constructor from entity preserves non-null status")
    void testConstructorFromEntityHiddenStatus() {
        final var entity = new CommentEntity();
        entity.publicId = "c-2";
        entity.content = "Hidden";
        entity.status = CommentDto.CommentStatus.HIDDEN;

        final var dto = new CommentViewDto(entity);
        assertEquals(CommentDto.CommentStatus.HIDDEN, dto.status);
    }

    @Test
    @DisplayName("toCommentDto maps fields correctly")
    void testToCommentDto() {
        final var dto = new CommentViewDto();
        dto.publicId = "comment-1";
        dto.content = "Hello";
        dto.exercisePublicId = "ex-1";
        dto.exerciseId = 5L;
        dto.parentPublicId = "parent-1";
        dto.parentId = 10L;
        dto.sessionId = "sess-1";

        final var result = dto.toCommentDto();
        assertNotNull(result);
        assertEquals("comment-1", result.publicId);
        assertEquals("Hello", result.content);
        assertEquals("ex-1", result.exercisePublicId);
        assertEquals(5L, result.exerciseId);
        assertEquals("parent-1", result.parentCommentPublicId);
        assertEquals(10L, result.parentCommentId);
        assertEquals("sess-1", result.sessionId);
    }
}
