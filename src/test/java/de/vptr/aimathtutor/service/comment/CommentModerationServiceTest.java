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
import de.vptr.aimathtutor.service.PermissionService;
import de.vptr.aimathtutor.util.TestCommentFactory;
import io.quarkus.test.InjectMock;
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

    @InjectMock
    PermissionService permissionService;

    @Test
    @DisplayName("Should throw NOT_FOUND for non-existent comment")
    @TestTransaction
    void shouldThrowNotFoundForNonExistentComment() {
        final UserEntity moderator = this.userRepository.findById(1L);
        final var ex = assertThrows(WebApplicationException.class, () -> this.moderationService
                .moderateComment("00000000000000000000000000", "HIDE", moderator.id, "reason"));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), ex.getResponse().getStatus());
    }

    @Test
    @DisplayName("Should hide comment and set moderation fields")
    @TestTransaction
    void shouldHideComment() {
        final var fixture = this.createModerationFixture();

        this.moderationService.moderateComment(fixture.comment().publicId, "HIDE", fixture.moderator().id,
                "Offensive content");

        assertEquals(CommentStatus.HIDDEN, fixture.comment().status);
        assertEquals("Offensive content", fixture.comment().moderationReason);
        assertEquals("HIDE", fixture.comment().moderationAction);
        assertNotNull(fixture.comment().moderatedAt);
        assertEquals(fixture.moderator().id, fixture.comment().moderator.id);
    }

    @Test
    @DisplayName("Should show comment and clear flags")
    @TestTransaction
    void shouldShowComment() {
        final var fixture = this.createModerationFixture();
        fixture.comment().flagsCount = 3;
        fixture.comment().deletedBy = fixture.moderator();
        fixture.comment().deletedAt = LocalDateTime.now();

        this.moderationService.moderateComment(fixture.comment().publicId, "SHOW", fixture.moderator().id, "Approved");

        assertEquals(CommentStatus.VISIBLE, fixture.comment().status);
        assertEquals(0, fixture.comment().flagsCount);
        assertNull(fixture.comment().deletedBy);
        assertNull(fixture.comment().deletedAt);
    }

    @Test
    @DisplayName("Should restore deleted comment")
    @TestTransaction
    void shouldRestoreComment() {
        final var fixture = this.createModerationFixture();
        fixture.comment().status = CommentStatus.DELETED;

        this.moderationService.moderateComment(fixture.comment().publicId, "RESTORE", fixture.moderator().id,
                "Restored");

        assertEquals(CommentStatus.VISIBLE, fixture.comment().status);
        assertEquals(0, fixture.comment().flagsCount);
        assertEquals("RESTORE", fixture.comment().moderationAction);
    }

    @Test
    @DisplayName("Should delete comment and set deleted fields")
    @TestTransaction
    void shouldDeleteComment() {
        final var fixture = this.createModerationFixture();

        this.moderationService.moderateComment(fixture.comment().publicId, "DELETE", fixture.moderator().id, "Spam");

        assertEquals(CommentStatus.DELETED, fixture.comment().status);
        assertEquals("DELETE", fixture.comment().moderationAction);
        assertNotNull(fixture.comment().deletedAt);
        assertEquals(fixture.moderator().id, fixture.comment().deletedBy.id);
    }

    @Test
    @DisplayName("Should throw ValidationException for invalid action")
    @TestTransaction
    void shouldThrowForInvalidAction() {
        final var fixture = this.createModerationFixture();

        assertThrows(ValidationException.class, () -> this.moderationService.moderateComment(fixture.comment().publicId,
                "INVALID", fixture.moderator().id, "reason"));
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when moderator is not found")
    @TestTransaction
    void shouldThrowBadRequestWhenModeratorNotFound() {
        final var fixture = this.createModerationFixture();

        final var ex = assertThrows(WebApplicationException.class,
                () -> this.moderationService.moderateComment(fixture.comment().publicId, "HIDE", 99_999L, "reason"));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
    }

    @Test
    @DisplayName("Should throw ValidationException for reason exceeding 500 chars")
    @TestTransaction
    void shouldThrowForLongReason() {
        final var fixture = this.createModerationFixture();

        final String longReason = "a".repeat(501);
        assertThrows(ValidationException.class, () -> this.moderationService.moderateComment(fixture.comment().publicId,
                "HIDE", fixture.moderator().id, longReason));
    }

    @Test
    @DisplayName("Should throw ValidationException for null action")
    @TestTransaction
    void shouldThrowForNullAction() {
        final var fixture = this.createModerationFixture();

        assertThrows(ValidationException.class, () -> this.moderationService.moderateComment(fixture.comment().publicId,
                null, fixture.moderator().id, "reason"));
    }

    private record ModerationFixture(UserEntity moderator, CommentEntity comment) {
    }

    private ModerationFixture createModerationFixture() {
        final UserEntity moderator = this.userRepository.findById(1L);
        final UserEntity author = this.userRepository.findById(3L);
        final ExerciseEntity exercise = this.exerciseRepository.findById(1L);
        final var comment = TestCommentFactory.createComment(this.commentRepository, exercise, author);
        return new ModerationFixture(moderator, comment);
    }
}
