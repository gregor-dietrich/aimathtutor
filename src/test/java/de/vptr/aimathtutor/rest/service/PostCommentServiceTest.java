package de.vptr.aimathtutor.rest.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.aimathtutor.rest.entity.PostCommentEntity;
import jakarta.validation.ValidationException;

@ExtendWith(MockitoExtension.class)
class PostCommentServiceTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private PostCommentService postCommentService;

    @Test
    @DisplayName("Should throw ValidationException when creating comment with null content")
    void shouldThrowValidationExceptionWhenCreatingCommentWithNullContent() {
        PostCommentEntity comment = new PostCommentEntity();
        comment.content = null;

        assertThrows(ValidationException.class, () -> {
            postCommentService.createComment(comment, "testuser");
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating comment with empty content")
    void shouldThrowValidationExceptionWhenCreatingCommentWithEmptyContent() {
        PostCommentEntity comment = new PostCommentEntity();
        comment.content = "";

        assertThrows(ValidationException.class, () -> {
            postCommentService.createComment(comment, "testuser");
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating comment with whitespace content")
    void shouldThrowValidationExceptionWhenCreatingCommentWithWhitespaceContent() {
        PostCommentEntity comment = new PostCommentEntity();
        comment.content = "   ";

        assertThrows(ValidationException.class, () -> {
            postCommentService.createComment(comment, "testuser");
        });
    }
}
