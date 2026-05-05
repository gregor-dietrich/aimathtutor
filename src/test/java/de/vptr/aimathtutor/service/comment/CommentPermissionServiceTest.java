package de.vptr.aimathtutor.service.comment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.CommentEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@QuarkusTest
class CommentPermissionServiceTest {

    @Inject
    CommentPermissionService permissionService;

    @Test
    @DisplayName("Should identify author correctly")
    void shouldIdentifyAuthor() {
        final UserEntity author = new UserEntity();
        author.id = 1L;

        final CommentEntity comment = new CommentEntity();
        comment.user = author;

        assertTrue(this.permissionService.isAuthor(comment, author));
    }

    @Test
    @DisplayName("Should not identify non-author as author")
    void shouldNotIdentifyNonAuthor() {
        final UserEntity author = new UserEntity();
        author.id = 1L;

        final UserEntity stranger = new UserEntity();
        stranger.id = 2L;

        final CommentEntity comment = new CommentEntity();
        comment.user = author;

        assertFalse(this.permissionService.isAuthor(comment, stranger));
    }

    @Test
    @DisplayName("Should allow author to pass verifyIsAuthorOrThrow")
    void shouldAllowAuthor() {
        final UserEntity author = new UserEntity();
        author.id = 1L;

        final CommentEntity comment = new CommentEntity();
        comment.user = author;

        // Should not throw
        this.permissionService.verifyIsAuthorOrThrow(comment, author);
    }

    @Test
    @DisplayName("Should forbid stranger from passing verifyIsAuthorOrThrow")
    void shouldForbidStranger() {
        final UserEntity author = new UserEntity();
        author.id = 1L;

        final UserEntity stranger = new UserEntity();
        stranger.id = 3L;

        final CommentEntity comment = new CommentEntity();
        comment.user = author;

        final var ex = assertThrows(WebApplicationException.class,
                () -> this.permissionService.verifyIsAuthorOrThrow(comment, stranger));
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getResponse().getStatus());
    }
}
