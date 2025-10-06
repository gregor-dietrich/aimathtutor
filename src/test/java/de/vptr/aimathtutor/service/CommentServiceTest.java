package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.aimathtutor.entity.CommentEntity;
import de.vptr.aimathtutor.service.CommentService;
import de.vptr.aimathtutor.service.UserService;
import jakarta.validation.ValidationException;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private CommentService commentService;

    @Test
    @DisplayName("Should throw ValidationException when creating comment with null content")
    void shouldThrowValidationExceptionWhenCreatingCommentWithNullContent() {
        final CommentEntity comment = new CommentEntity();
        comment.content = null;

        assertThrows(ValidationException.class, () -> {
            this.commentService.createComment(comment, "testuser");
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating comment with empty content")
    void shouldThrowValidationExceptionWhenCreatingCommentWithEmptyContent() {
        final CommentEntity comment = new CommentEntity();
        comment.content = "";

        assertThrows(ValidationException.class, () -> {
            this.commentService.createComment(comment, "testuser");
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating comment with whitespace content")
    void shouldThrowValidationExceptionWhenCreatingCommentWithWhitespaceContent() {
        final CommentEntity comment = new CommentEntity();
        comment.content = "   ";

        assertThrows(ValidationException.class, () -> {
            this.commentService.createComment(comment, "testuser");
        });
    }
}
