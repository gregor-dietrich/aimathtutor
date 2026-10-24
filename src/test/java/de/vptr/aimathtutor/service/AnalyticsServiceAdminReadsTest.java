package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.time.ZoneId;
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
class AnalyticsServiceAdminReadsTest {

    // The mocked PermissionService accepts any credentials; the service's own checks are tested with it
    // (the guard itself is tested in PermissionServiceTest)
    private static final SessionCredentials CREDENTIALS =
            new SessionCredentials("admin-public-id", "stamp", "session-key");

    @Inject
    AnalyticsService analyticsService;

    @InjectMock
    PermissionService permissionService;

    private static Named<Consumer<AnalyticsService>> read(final String name, final Consumer<AnalyticsService> call) {
        return Named.of(name, call);
    }

    /** Every guarded admin read, called once. */
    static Stream<Named<Consumer<AnalyticsService>>> guardedReads() {
        final var start = LocalDateTime.now(ZoneId.systemDefault()).minusDays(1);
        final var end = LocalDateTime.now(ZoneId.systemDefault());
        return Stream.of(read("getAllSessions", s -> s.getAllSessions(CREDENTIALS)),
                read("getSessionsByDateRange", s -> s.getSessionsByDateRange(CREDENTIALS, start, end)),
                read("getSessionBySessionId", s -> s.getSessionBySessionId(CREDENTIALS, "no-such-session")),
                read("getAiInteractionsBySession", s -> s.getAiInteractionsBySession(CREDENTIALS, "no-such-session")),
                read("getAllUsersProgressSummary", s -> s.getAllUsersProgressSummary(CREDENTIALS)),
                read("searchSessions", s -> s.searchSessions(CREDENTIALS, "x")),
                read("getUsersProgressSummaryByDateRange",
                        s -> s.getUsersProgressSummaryByDateRange(CREDENTIALS, start, end)),
                read("getUsersProgressSummaryByUsernameSearch",
                        s -> s.getUsersProgressSummaryByUsernameSearch(CREDENTIALS, "x")),
                read("getRecentSessions", s -> s.getRecentSessions(CREDENTIALS, 5)),
                read("getTopStudentsByCompletion", s -> s.getTopStudentsByCompletion(CREDENTIALS, 5)));
    }

    @ParameterizedTest
    @MethodSource("guardedReads")
    @DisplayName("An admin read requires the administration view exactly once")
    @TestTransaction
    void shouldRequireAdminViewOnce(final Consumer<AnalyticsService> call) {
        call.accept(this.analyticsService);

        verify(this.permissionService).requireAdminView(CREDENTIALS);
    }

    @ParameterizedTest
    @MethodSource("guardedReads")
    @DisplayName("An admin read propagates the refusal")
    @TestTransaction
    void shouldPropagateAdminViewRefusal(final Consumer<AnalyticsService> call) {
        doThrow(new PermissionDeniedException("view the administration")).when(this.permissionService)
                .requireAdminView(CREDENTIALS);

        assertThrows(PermissionDeniedException.class, () -> call.accept(this.analyticsService));
    }

    @Test
    @DisplayName("A blank progress search requires the administration view once, though it lists every user")
    @TestTransaction
    void shouldRequireAdminViewOnceForBlankProgressSearch() {
        this.analyticsService.getUsersProgressSummaryByUsernameSearch(CREDENTIALS, "");

        verify(this.permissionService).requireAdminView(CREDENTIALS);
    }

    @Test
    @DisplayName("A session that ended refuses the read")
    @TestTransaction
    void shouldRefuseReadOfEndedSession() {
        doThrow(new ValidationException(AppConstants.SESSION_ENDED_MESSAGE)).when(this.permissionService)
                .requireAdminView(CREDENTIALS);

        assertThrows(ValidationException.class, () -> this.analyticsService.getSessionBySessionId(CREDENTIALS, "any"));
    }

    @Test
    @DisplayName("The aggregates the dashboard reads without credentials never ask for the administration view")
    @TestTransaction
    void shouldNotGuardAnonymousAggregates() {
        this.analyticsService.getTotalSessionsCount();
        this.analyticsService.getCompletedSessionsCount();
        this.analyticsService.getActiveStudentsCount();
        this.analyticsService.getTodaySessionsCount();
        this.analyticsService.getUserCount();
        this.analyticsService.getPublishedExerciseCount();
        this.analyticsService.getDailySessionCounts(7);
        this.analyticsService.getCompletedSessionsByExercise();
        this.analyticsService.getCompletionRateHistogram();
        this.analyticsService.getHintUsageBuckets();
        this.analyticsService.getAverageSuccessRate();
        this.analyticsService.getTrendData();

        verify(this.permissionService, never()).requireAdminView(any());
    }
}
