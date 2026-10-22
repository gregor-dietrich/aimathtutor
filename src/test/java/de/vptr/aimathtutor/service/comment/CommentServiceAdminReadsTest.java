package de.vptr.aimathtutor.service.comment;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.CommentDto.CommentStatus;
import de.vptr.aimathtutor.dto.SessionCredentials;
import de.vptr.aimathtutor.exception.PermissionDeniedException;
import de.vptr.aimathtutor.service.security.PermissionService;
import de.vptr.aimathtutor.util.AppConstants;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.validation.ValidationException;

@QuarkusTest
@SuppressWarnings("NullAway")
class CommentServiceAdminReadsTest {

    // The mocked PermissionService accepts any credentials; the guard's own checks are tested with it
    private static final SessionCredentials CREDENTIALS =
            new SessionCredentials("admin-public-id", "stamp", "session-key");

    @Inject
    private CommentService commentService;

    @InjectMock
    private PermissionService permissionService;

    @Test
    @DisplayName("Admin comment reads require the administration view once per call, also when they delegate")
    @TestTransaction
    void shouldRequireAdminViewForCommentReads() {
        this.commentService.getAllComments(CREDENTIALS);
        this.commentService.searchComments(CREDENTIALS, "");
        this.commentService.findByStatus(CREDENTIALS, CommentStatus.VISIBLE);
        this.commentService.findByExerciseId(CREDENTIALS, 1L);

        verify(this.permissionService, times(4)).requireAdminView(CREDENTIALS);
    }

    @Test
    @DisplayName("Admin comment reads propagate the refusal and return no data")
    @TestTransaction
    void shouldRefuseCommentReadsWithoutAdminView() {
        doThrow(new PermissionDeniedException("view the administration")).when(this.permissionService)
                .requireAdminView(CREDENTIALS);

        assertThrows(PermissionDeniedException.class, () -> this.commentService.getAllComments(CREDENTIALS));
        assertThrows(PermissionDeniedException.class, () -> this.commentService.searchComments(CREDENTIALS, "x"));
        assertThrows(PermissionDeniedException.class, () -> this.commentService.findFlaggedComments(CREDENTIALS, 0));
    }

    @Test
    @DisplayName("A session that ended refuses the comment read")
    @TestTransaction
    void shouldRefuseCommentReadOfEndedSession() {
        doThrow(new ValidationException(AppConstants.SESSION_ENDED_MESSAGE)).when(this.permissionService)
                .requireAdminView(CREDENTIALS);

        assertThrows(ValidationException.class, () -> this.commentService.findByExerciseId(CREDENTIALS, 1L));
    }
}
