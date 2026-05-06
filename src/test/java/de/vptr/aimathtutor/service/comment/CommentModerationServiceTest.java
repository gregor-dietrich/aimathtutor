package de.vptr.aimathtutor.service.comment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.CommentDto.CommentStatus;
import de.vptr.aimathtutor.entity.CommentEntity;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.repository.CommentRepository;
import de.vptr.aimathtutor.repository.ExerciseRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@QuarkusTest
class CommentModerationServiceTest {

    @Inject
    CommentModerationService moderationService;

    @Inject
    CommentRepository commentRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    ExerciseRepository exerciseRepository;

    @Test
    @DisplayName("Should throw NOT_FOUND for non-existent comment")
    @TestTransaction
    void shouldThrowNotFoundForNonExistentComment() {
        final UserEntity moderator = this.userRepository.findById(1L);
        final var ex = assertThrows(WebApplicationException.class,
                () -> this.moderationService.moderateComment("00000000000000000000000000", "HIDE", moderator.id, "reason"));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), ex.getResponse().getStatus());
    }

    @Test
    @DisplayName("Should throw FORBIDDEN for non-moderator")
    @TestTransaction
    void shouldThrowForbiddenForNonModerator() {
        final UserEntity regularUser = this.userRepository.findById(3L); // Student rank
        final ExerciseEntity exercise = this.exerciseRepository.findById(1L);
        final var comment = this.createComment(exercise, regularUser);

        final var ex = assertThrows(WebApplicationException.class,
                () -> this.moderationService.moderateComment(comment.publicId, "HIDE", regularUser.id, "reason"));
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getResponse().getStatus());
    }

    @Test
    @DisplayName("Should hide comment and set moderation fields")
    @TestTransaction
    void shouldHideComment() {
        final UserEntity moderator = this.userRepository.findById(1L);
        final UserEntity author = this.userRepository.findById(3L);
        final ExerciseEntity exercise = this.exerciseRepository.findById(1L);
        final var comment = this.createComment(exercise, author);

        this.moderationService.moderateComment(comment.publicId, "HIDE", moderator.id, "Offensive content");

        assertEquals(CommentStatus.HIDDEN, comment.status);
        assertEquals("Offensive content", comment.moderationReason);
        assertEquals("HIDE", comment.moderationAction);
        assertNotNull(comment.moderatedAt);
        assertEquals(moderator.id, comment.moderator.id);
    }

    @Test
    @DisplayName("Should show comment and clear flags")
    @TestTransaction
    void shouldShowComment() {
        final UserEntity moderator = this.userRepository.findById(1L);
        final UserEntity author = this.userRepository.findById(3L);
        final ExerciseEntity exercise = this.exerciseRepository.findById(1L);
        final var comment = this.createComment(exercise, author);
        comment.flagsCount = 3;
        comment.deletedBy = moderator;
        comment.deletedAt = LocalDateTime.now();

        this.moderationService.moderateComment(comment.publicId, "SHOW", moderator.id, "Approved");

        assertEquals(CommentStatus.VISIBLE, comment.status);
        assertEquals(0, comment.flagsCount);
        assertNull(comment.deletedBy);
        assertNull(comment.deletedAt);
    }

    @Test
    @DisplayName("Should restore deleted comment")
    @TestTransaction
    void shouldRestoreComment() {
        final UserEntity moderator = this.userRepository.findById(1L);
        final UserEntity author = this.userRepository.findById(3L);
        final ExerciseEntity exercise = this.exerciseRepository.findById(1L);
        final var comment = this.createComment(exercise, author);
        comment.status = CommentStatus.DELETED;

        this.moderationService.moderateComment(comment.publicId, "RESTORE", moderator.id, "Restored");

        assertEquals(CommentStatus.VISIBLE, comment.status);
        assertEquals(0, comment.flagsCount);
        assertEquals("RESTORE", comment.moderationAction);
    }

    @Test
    @DisplayName("Should delete comment and set deleted fields")
    @TestTransaction
    void shouldDeleteComment() {
        final UserEntity moderator = this.userRepository.findById(1L);
        final UserEntity author = this.userRepository.findById(3L);
        final ExerciseEntity exercise = this.exerciseRepository.findById(1L);
        final var comment = this.createComment(exercise, author);

        this.moderationService.moderateComment(comment.publicId, "DELETE", moderator.id, "Spam");

        assertEquals(CommentStatus.DELETED, comment.status);
        assertEquals("DELETE", comment.moderationAction);
        assertNotNull(comment.deletedAt);
        assertEquals(moderator.id, comment.deletedBy.id);
    }

    @Test
    @DisplayName("Should throw ValidationException for invalid action")
    @TestTransaction
    void shouldThrowForInvalidAction() {
        final UserEntity moderator = this.userRepository.findById(1L);
        final UserEntity author = this.userRepository.findById(3L);
        final ExerciseEntity exercise = this.exerciseRepository.findById(1L);
        final var comment = this.createComment(exercise, author);

        assertThrows(ValidationException.class,
                () -> this.moderationService.moderateComment(comment.publicId, "INVALID", moderator.id, "reason"));
    }

    @Test
    @DisplayName("Should throw ValidationException for reason exceeding 500 chars")
    @TestTransaction
    void shouldThrowForLongReason() {
        final UserEntity moderator = this.userRepository.findById(1L);
        final UserEntity author = this.userRepository.findById(3L);
        final ExerciseEntity exercise = this.exerciseRepository.findById(1L);
        final var comment = this.createComment(exercise, author);

        final String longReason = "a".repeat(501);
        assertThrows(ValidationException.class,
                () -> this.moderationService.moderateComment(comment.publicId, "HIDE", moderator.id, longReason));
    }

    @Test
    @DisplayName("Should throw ValidationException for null action")
    @TestTransaction
    void shouldThrowForNullAction() {
        final UserEntity moderator = this.userRepository.findById(1L);
        final UserEntity author = this.userRepository.findById(3L);
        final ExerciseEntity exercise = this.exerciseRepository.findById(1L);
        final var comment = this.createComment(exercise, author);

        assertThrows(ValidationException.class,
                () -> this.moderationService.moderateComment(comment.publicId, null, moderator.id, "reason"));
    }

    private CommentEntity createComment(final ExerciseEntity exercise, final UserEntity user) {
        final var comment = new CommentEntity();
        comment.content = "Test comment";
        comment.exercise = exercise;
        comment.user = user;
        comment.status = CommentStatus.VISIBLE;
        this.commentRepository.persist(comment);
        return comment;
    }
}
