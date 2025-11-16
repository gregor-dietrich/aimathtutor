package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.CommentEntity;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;

@QuarkusTest
class CommentServiceTest {

    @Inject
    private CommentService commentService;

    @Test
    @DisplayName("Should throw ValidationException when creating comment with null content")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingCommentWithNullContent() {
        final CommentEntity comment = new CommentEntity();
        comment.content = null;

        assertThrows(ValidationException.class, () -> {
            this.commentService.createComment(comment, "testuser");
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating comment with empty content")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingCommentWithEmptyContent() {
        final CommentEntity comment = new CommentEntity();
        comment.content = "";

        assertThrows(ValidationException.class, () -> {
            this.commentService.createComment(comment, "testuser");
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating comment with whitespace content")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingCommentWithWhitespaceContent() {
        final CommentEntity comment = new CommentEntity();
        comment.content = "   ";

        assertThrows(ValidationException.class, () -> {
            this.commentService.createComment(comment, "testuser");
        });
    }
}
