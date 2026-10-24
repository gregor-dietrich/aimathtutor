package de.vptr.aimathtutor.service;

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
class ExerciseServiceAdminReadsTest {

    // The mocked PermissionService accepts any credentials; the service's own checks are tested with it
    // (the guard itself is tested in PermissionServiceTest)
    private static final SessionCredentials CREDENTIALS =
            new SessionCredentials("admin-public-id", "stamp", "session-key");

    @Inject
    private ExerciseService exerciseService;

    @InjectMock
    private PermissionService permissionService;

    private static Named<Consumer<ExerciseService>> read(final String name, final Consumer<ExerciseService> call) {
        return Named.of(name, call);
    }

    /** Every guarded admin read, called once. */
    static Stream<Named<Consumer<ExerciseService>>> guardedReads() {
        return Stream.of(read("getAllExercises", s -> s.getAllExercises(CREDENTIALS)),
                read("getAllExercises paged", s -> s.getAllExercises(CREDENTIALS, 0, 10)),
                read("searchExercises", s -> s.searchExercises(CREDENTIALS, "x")),
                read("findByDateRange", s -> s.findByDateRange(CREDENTIALS, "2024-01-01", "2024-01-02")),
                read("findByUserId", s -> s.findByUserId(CREDENTIALS, 1L)));
    }

    @ParameterizedTest
    @MethodSource("guardedReads")
    @DisplayName("An admin exercise read requires the administration view exactly once")
    @TestTransaction
    void shouldRequireAdminViewOnce(final Consumer<ExerciseService> call) {
        call.accept(this.exerciseService);

        verify(this.permissionService).requireAdminView(CREDENTIALS);
    }

    @ParameterizedTest
    @MethodSource("guardedReads")
    @DisplayName("An admin exercise read propagates the refusal")
    @TestTransaction
    void shouldPropagateAdminViewRefusal(final Consumer<ExerciseService> call) {
        doThrow(new PermissionDeniedException("view the administration")).when(this.permissionService)
                .requireAdminView(CREDENTIALS);

        assertThrows(PermissionDeniedException.class, () -> call.accept(this.exerciseService));
    }

    @Test
    @DisplayName("A blank search requires the administration view once, though it lists every exercise")
    @TestTransaction
    void shouldRequireAdminViewOnceForBlankSearch() {
        this.exerciseService.searchExercises(CREDENTIALS, "");

        verify(this.permissionService).requireAdminView(CREDENTIALS);
    }

    @Test
    @DisplayName("A date range with null dates requires the administration view once")
    @TestTransaction
    void shouldRequireAdminViewOnceForNullDateRange() {
        this.exerciseService.findByDateRange(CREDENTIALS, null, null);

        verify(this.permissionService).requireAdminView(CREDENTIALS);
    }

    @Test
    @DisplayName("A date range that cannot be parsed requires the administration view once")
    @TestTransaction
    void shouldRequireAdminViewOnceForUnparsableDateRange() {
        this.exerciseService.findByDateRange(CREDENTIALS, "not-a-date", "also-not-a-date");

        verify(this.permissionService).requireAdminView(CREDENTIALS);
    }

    @Test
    @DisplayName("A session that ended refuses the exercise read")
    @TestTransaction
    void shouldRefuseExerciseReadOfEndedSession() {
        doThrow(new ValidationException(AppConstants.SESSION_ENDED_MESSAGE)).when(this.permissionService)
                .requireAdminView(CREDENTIALS);

        assertThrows(ValidationException.class, () -> this.exerciseService.findByUserId(CREDENTIALS, 1L));
    }
}
