package de.vptr.aimathtutor.service.comment;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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

    // The mocked PermissionService accepts any credentials; the service's own checks are tested with it
    // (the guard itself is tested in PermissionServiceTest)
    private static final SessionCredentials CREDENTIALS =
            new SessionCredentials("admin-public-id", "stamp", "session-key");

    @Inject
    private CommentService commentService;

    @InjectMock
    private PermissionService permissionService;

    private static Named<Consumer<CommentService>> read(final String name, final Consumer<CommentService> call) {
        return Named.of(name, call);
    }

    /** Every guarded admin read, called once. */
    static Stream<Named<Consumer<CommentService>>> guardedReads() {
        return Stream.of(read("getAllComments", s -> s.getAllComments(CREDENTIALS)),
                read("getAllComments paged", s -> s.getAllComments(CREDENTIALS, 0, 10)),
                read("searchComments", s -> s.searchComments(CREDENTIALS, "x")),
                read("findByStatus", s -> s.findByStatus(CREDENTIALS, CommentStatus.VISIBLE)),
                read("findByExerciseId", s -> s.findByExerciseId(CREDENTIALS, 1L)),
                read("findByExerciseId paged", s -> s.findByExerciseId(CREDENTIALS, 1L, 0, 10)),
                read("findByUserId", s -> s.findByUserId(CREDENTIALS, 1L)),
                read("findByDateRange", s -> s.findByDateRange(CREDENTIALS, "2026-01-01", "2026-01-31")),
                read("findFlaggedComments", s -> s.findFlaggedComments(CREDENTIALS, 0)));
    }

    @ParameterizedTest
    @MethodSource("guardedReads")
    @DisplayName("An admin comment read requires the administration view exactly once")
    @TestTransaction
    void shouldRequireAdminViewOnce(final Consumer<CommentService> call) {
        call.accept(this.commentService);

        verify(this.permissionService).requireAdminView(CREDENTIALS);
    }

    @ParameterizedTest
    @MethodSource("guardedReads")
    @DisplayName("An admin comment read propagates the refusal")
    @TestTransaction
    void shouldPropagateAdminViewRefusal(final Consumer<CommentService> call) {
        doThrow(new PermissionDeniedException("view the administration")).when(this.permissionService)
                .requireAdminView(CREDENTIALS);

        assertThrows(PermissionDeniedException.class, () -> call.accept(this.commentService));
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
