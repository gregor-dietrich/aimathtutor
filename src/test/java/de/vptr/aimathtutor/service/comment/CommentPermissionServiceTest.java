package de.vptr.aimathtutor.service.comment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.CommentEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserRankEntity;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@QuarkusTest
class CommentPermissionServiceTest {

    @Inject
    CommentPermissionService permissionService;

    @Test
    @DisplayName("Should identify moderator by exerciseEdit permission")
    void shouldIdentifyModeratorByExerciseEditPermission() {
        final UserRankEntity rank = new UserRankEntity();
        rank.exerciseEdit = true;

        final UserEntity user = new UserEntity();
        user.rank = rank;

        assertTrue(this.permissionService.isModerator(user));
    }

    @Test
    @DisplayName("Should identify moderator by adminView permission")
    void shouldIdentifyModeratorByAdminViewPermission() {
        final UserRankEntity rank = new UserRankEntity();
        rank.adminView = true;

        final UserEntity user = new UserEntity();
        user.rank = rank;

        assertTrue(this.permissionService.isModerator(user));
    }

    @Test
    @DisplayName("Should not identify regular user as moderator")
    void shouldNotIdentifyRegularUserAsModerator() {
        final UserRankEntity rank = new UserRankEntity();
        rank.exerciseEdit = false;
        rank.adminView = false;

        final UserEntity user = new UserEntity();
        user.rank = rank;

        assertFalse(this.permissionService.isModerator(user));
    }

    @Test
    @DisplayName("Should allow author to edit their own comment")
    void shouldAllowAuthorToEditOwnComment() {
        final UserEntity author = new UserEntity();
        author.id = 1L;

        final CommentEntity comment = new CommentEntity();
        comment.user = author;

        // Should not throw
        this.permissionService.verifyCanEdit(comment, author);
    }

    @Test
    @DisplayName("Should allow moderator to edit any comment")
    void shouldAllowModeratorToEditAnyComment() {
        final UserRankEntity rank = new UserRankEntity();
        rank.adminView = true;

        final UserEntity moderator = new UserEntity();
        moderator.id = 2L;
        moderator.rank = rank;

        final UserEntity author = new UserEntity();
        author.id = 1L;

        final CommentEntity comment = new CommentEntity();
        comment.user = author;

        // Should not throw
        this.permissionService.verifyCanEdit(comment, moderator);
    }

    @Test
    @DisplayName("Should forbid stranger from editing comment")
    void shouldForbidStrangerFromEditingComment() {
        final UserEntity author = new UserEntity();
        author.id = 1L;

        final UserEntity stranger = new UserEntity();
        stranger.id = 3L;

        final CommentEntity comment = new CommentEntity();
        comment.user = author;

        final var ex = assertThrows(WebApplicationException.class,
                () -> this.permissionService.verifyCanEdit(comment, stranger));
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getResponse().getStatus());
    }

    @Test
    @DisplayName("Should forbid hard delete by non-moderator")
    void shouldForbidHardDeleteByNonModerator() {
        final UserEntity author = new UserEntity();
        author.id = 1L;

        final CommentEntity comment = new CommentEntity();
        comment.user = author;

        final var ex = assertThrows(WebApplicationException.class,
                () -> this.permissionService.verifyCanDelete(comment, author, false));
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getResponse().getStatus());
    }

    @Test
    @DisplayName("Should allow soft delete by author")
    void shouldAllowSoftDeleteByAuthor() {
        final UserEntity author = new UserEntity();
        author.id = 1L;

        final CommentEntity comment = new CommentEntity();
        comment.user = author;

        // Should not throw
        this.permissionService.verifyCanDelete(comment, author, true);
    }

    @Test
    @DisplayName("Should allow hard delete by moderator")
    void shouldAllowHardDeleteByModerator() {
        final UserEntity author = new UserEntity();
        author.id = 1L;

        final CommentEntity comment = new CommentEntity();
        comment.user = author;

        final UserRankEntity rank = new UserRankEntity();
        rank.adminView = true;

        final UserEntity moderator = new UserEntity();
        moderator.id = 2L;
        moderator.rank = rank;

        // Should not throw
        this.permissionService.verifyCanDelete(comment, moderator, false);
    }
}
