package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.AiInteractionViewDto;
import de.vptr.aimathtutor.dto.ExerciseDto;
import de.vptr.aimathtutor.dto.ExerciseViewDto;
import de.vptr.aimathtutor.dto.SessionCredentials;
import de.vptr.aimathtutor.dto.StudentProgressSummaryDto;
import de.vptr.aimathtutor.dto.StudentSessionViewDto;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.StudentSessionEntity;
import de.vptr.aimathtutor.exception.PermissionDeniedException;
import de.vptr.aimathtutor.repository.ExerciseRepository;
import de.vptr.aimathtutor.repository.StudentSessionRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.security.AuthService;
import de.vptr.aimathtutor.service.security.PermissionService;
import de.vptr.aimathtutor.util.AppConstants;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;

@QuarkusTest
@SuppressWarnings("NullAway")
class AnalyticsServiceTest {

    // The mocked PermissionService accepts any credentials; the guard's own checks are tested with it
    private static final SessionCredentials CREDENTIALS =
            new SessionCredentials("admin-public-id", "stamp", "session-key");

    @Inject
    AnalyticsService analyticsService;

    @Inject
    GraspableMathService graspableMathService;

    @Inject
    ExerciseService exerciseService;

    @Inject
    UserRepository userRepository;

    @InjectMock
    PermissionService permissionService;

    @InjectMock
    AuthService authService;

    @Inject
    StudentSessionRepository studentSessionRepository;

    @Inject
    ExerciseRepository exerciseRepository;

    @BeforeEach
    @Transactional
    void setUpAuthMock() {
        final var teacher = this.userRepository.findByUsername("teacher");
        if (teacher != null) {
            when(this.authService.getUserId()).thenReturn(teacher.id);
            when(this.authService.getCurrentUserEntity()).thenReturn(teacher);
        }
    }

    private Long studentId() {
        final var user = this.userRepository.findByUsername("student1");
        assertNotNull(user, "Seeded student1 must exist");
        return user.id;
    }

    private Long createExercise() {
        final var suffix = UUID.randomUUID().toString().substring(0, 8);
        final var dto = new ExerciseDto("analytics_ex_" + suffix, "content " + suffix, null, true, false);
        final ExerciseViewDto created = this.exerciseService.createExercise(dto);
        return created.id;
    }

    /** Creates a session as the given user, whom the mocked auth service then reports as the current user. */
    private String createSessionFor(final Long userId, final Long exerciseId) {
        when(this.authService.requireCurrentUser()).thenReturn(this.userRepository.findById(userId));
        return this.graspableMathService.createSession(exerciseId);
    }

    private StudentSessionEntity createAndPersistSession(final Long studentId, final Long exerciseId) {
        final var exercise = this.exerciseRepository.findById(exerciseId);
        assertNotNull(exercise, "Created exercise must exist");
        final var user = this.userRepository.findById(studentId);
        assertNotNull(user, "Seeded student1 must exist");
        final var session = new StudentSessionEntity();
        session.sessionId = UUID.randomUUID().toString();
        session.user = user;
        session.exercise = exercise;
        this.studentSessionRepository.persist(session);
        return session;
    }

    @Test
    @TestTransaction
    @DisplayName("getAllSessions returns a non-null list")
    void testGetAllSessions() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        final String sessionId = this.createSessionFor(studentId, exerciseId);

        final List<StudentSessionViewDto> sessions = this.analyticsService.getAllSessions(CREDENTIALS);
        assertNotNull(sessions);
        assertTrue(sessions.stream().anyMatch(s -> sessionId.equals(s.sessionId)),
                "Seeded session should appear in the list");
    }

    @Test
    @TestTransaction
    @DisplayName("getSessionsByUserAndExercise filters by both user and exercise")
    void testGetSessionsByUserAndExercise() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        final String sessionId = this.createSessionFor(studentId, exerciseId);

        final var otherStudent = this.userRepository.findByUsername("student2");
        assertNotNull(otherStudent, "Seeded student2 must exist");
        final Long otherStudentId = otherStudent.id;
        final Long otherExerciseId = this.createExercise();
        final String otherSessionId = this.createSessionFor(otherStudentId, otherExerciseId);

        final Long differentExerciseId = this.createExercise();
        final String sameUserDiffExerciseSessionId = this.createSessionFor(studentId, differentExerciseId);

        final String diffUserSameExerciseSessionId = this.createSessionFor(otherStudentId, exerciseId);

        final List<StudentSessionViewDto> sessions =
                this.analyticsService.getSessionsByUserAndExercise(studentId, exerciseId);
        assertNotNull(sessions);
        assertFalse(sessions.isEmpty());
        assertTrue(sessions.stream().anyMatch(s -> sessionId.equals(s.sessionId)),
                "Expected session should be present");
        final var exercise = this.exerciseService.findById(exerciseId).orElseThrow();
        final String expectedExercisePublicId = exercise.publicId;
        for (final StudentSessionViewDto s : sessions) {
            assertEquals("student1", s.username, "All sessions should belong to student1");
            assertEquals(expectedExercisePublicId, s.exercisePublicId, "All sessions should belong to the exercise");
        }
        assertFalse(sessions.stream().anyMatch(s -> sameUserDiffExerciseSessionId.equals(s.sessionId)),
                "Session with same user but different exercise should be excluded");
        assertFalse(sessions.stream().anyMatch(s -> diffUserSameExerciseSessionId.equals(s.sessionId)),
                "Session with different user but same exercise should be excluded");
        assertFalse(sessions.stream().anyMatch(s -> otherSessionId.equals(s.sessionId)),
                "Session from other student/exercise should not appear");
    }

    @Test
    @TestTransaction
    @DisplayName("getSessionsByDateRange with both bounds returns sessions within range")
    void testGetSessionsByDateRange_bothBounds() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        final String sessionId = this.createSessionFor(studentId, exerciseId);

        final LocalDateTime start = LocalDateTime.now(ZoneId.systemDefault()).minusMinutes(1);
        final LocalDateTime end = LocalDateTime.now(ZoneId.systemDefault()).plusMinutes(1);
        final List<StudentSessionViewDto> sessions =
                this.analyticsService.getSessionsByDateRange(CREDENTIALS, start, end);
        assertNotNull(sessions);
        assertTrue(sessions.stream().anyMatch(s -> sessionId.equals(s.sessionId)),
                "Seeded session should be returned within the date range");
    }

    @Test
    @TestTransaction
    @DisplayName("getSessionsByDateRange with null bounds returns all sessions")
    void testGetSessionsByDateRange_nullBounds() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        final String sessionId = this.createSessionFor(studentId, exerciseId);

        final List<StudentSessionViewDto> sessions =
                this.analyticsService.getSessionsByDateRange(CREDENTIALS, null, null);
        assertNotNull(sessions);
        assertTrue(sessions.stream().anyMatch(s -> sessionId.equals(s.sessionId)),
                "Seeded session should appear when bounds are null");
    }

    @Test
    @TestTransaction
    @DisplayName("getSessionBySessionId returns the session matching the string ID")
    void testGetSessionBySessionId() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        final String sessionId = this.createSessionFor(studentId, exerciseId);

        final StudentSessionViewDto found = this.analyticsService.getSessionBySessionId(CREDENTIALS, sessionId);
        assertNotNull(found);
        assertEquals(sessionId, found.sessionId);
    }

    @Test
    @TestTransaction
    @DisplayName("getSessionBySessionId returns null for unknown session ID")
    void testGetSessionBySessionId_notFound() {
        final StudentSessionViewDto result =
                this.analyticsService.getSessionBySessionId(CREDENTIALS, "nonexistent-session-id");
        assertNull(result);
    }

    @Test
    @TestTransaction
    @DisplayName("getAiInteractionsBySession returns empty list for unknown session")
    void testGetAiInteractionsBySession_notFound() {
        final List<AiInteractionViewDto> interactions =
                this.analyticsService.getAiInteractionsBySession(CREDENTIALS, "no-such-session");
        assertNotNull(interactions);
        assertTrue(interactions.isEmpty());
    }

    @Test
    @TestTransaction
    @DisplayName("getAllUsersProgressSummary returns at least one entry for seeded users")
    void testGetAllUsersProgressSummary() {
        final List<StudentProgressSummaryDto> summaries = this.analyticsService.getAllUsersProgressSummary(CREDENTIALS);
        assertNotNull(summaries);
        assertFalse(summaries.isEmpty(), "Seeded users should produce at least one summary");
    }

    @Test
    @TestTransaction
    @DisplayName("getTotalSessionsCount increases by 1 when adding a session")
    void testGetTotalSessionsCount() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        final long before = this.analyticsService.getTotalSessionsCount();
        this.createAndPersistSession(studentId, exerciseId);
        final long after = this.analyticsService.getTotalSessionsCount();
        assertEquals(before + 1, after, "Adding one session should increase total count by 1");
    }

    @Test
    @TestTransaction
    @DisplayName("getCompletedSessionsCount increases by 1 when adding a completed session")
    void testGetCompletedSessionsCount() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        final long before = this.analyticsService.getCompletedSessionsCount();
        final var session = this.createAndPersistSession(studentId, exerciseId);
        session.completed = true;
        final long after = this.analyticsService.getCompletedSessionsCount();
        assertEquals(before + 1, after, "Adding one completed session should increase completed count by 1");
    }

    @Test
    @TestTransaction
    @DisplayName("getActiveStudentsCount increases by 1 when adding a student with an active session")
    void testGetActiveStudentsCount() {
        final var admin = this.userRepository.findByUsername("admin");
        assertNotNull(admin, "Seeded admin must exist");
        final Long adminId = admin.id;
        for (final var existing : this.studentSessionRepository.findByUserId(adminId)) {
            this.studentSessionRepository.deleteById(existing.id);
        }
        final Long exerciseId = this.createExercise();
        final long before = this.analyticsService.getActiveStudentsCount();
        final var exercise = this.exerciseRepository.findById(exerciseId);
        assertNotNull(exercise, "Created exercise must exist");
        final var user = this.userRepository.findById(adminId);
        assertNotNull(user, "Seeded admin must exist");
        final var session = new StudentSessionEntity();
        session.sessionId = UUID.randomUUID().toString();
        session.user = user;
        session.exercise = exercise;
        session.startTime = LocalDateTime.now(ZoneId.systemDefault());
        this.studentSessionRepository.persist(session);
        final long after = this.analyticsService.getActiveStudentsCount();
        assertEquals(before + 1, after, "Adding one active student should increase active count by 1");
    }

    @Test
    @TestTransaction
    @DisplayName("getTodaySessionsCount increases by 1 when adding a session with today's date")
    void testGetTodaySessionsCount() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        final long before = this.analyticsService.getTodaySessionsCount();
        final var session = this.createAndPersistSession(studentId, exerciseId);
        session.startTime = LocalDateTime.now(ZoneId.systemDefault());
        final long after = this.analyticsService.getTodaySessionsCount();
        assertEquals(before + 1, after, "Adding one session with today's date should increase today count by 1");
    }

    @Test
    @TestTransaction
    @DisplayName("searchSessions returns empty list for blank term")
    void testSearchSessions_blank() {
        final List<StudentSessionViewDto> results = this.analyticsService.searchSessions(CREDENTIALS, "");
        assertNotNull(results);
        assertTrue(results.isEmpty(), "Blank search term should return empty list");
    }

    @Test
    @TestTransaction
    @DisplayName("searchSessions returns empty list for nonsense term")
    void testSearchSessions_noMatch() {
        final List<StudentSessionViewDto> results =
                this.analyticsService.searchSessions(CREDENTIALS, "zzz_no_match_xyz_999");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @TestTransaction
    @DisplayName("searchSessions returns matching sessions by username")
    void testSearchSessions_byUsername() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        this.createSessionFor(studentId, exerciseId);

        final var otherStudent = this.userRepository.findByUsername("student2");
        assertNotNull(otherStudent, "Seeded student2 must exist");
        final Long otherStudentId = otherStudent.id;
        final Long otherExerciseId = this.createExercise();
        this.createSessionFor(otherStudentId, otherExerciseId);

        final List<StudentSessionViewDto> results = this.analyticsService.searchSessions(CREDENTIALS, "student1");
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Search by username should find student1 sessions");
        assertTrue(results.stream().allMatch(s -> "student1".equals(s.username)),
                "Results should only contain sessions for student1");
    }

    @Test
    @TestTransaction
    @DisplayName("getSessionsByUserGroupedByExercise returns empty map for null user")
    void testGetSessionsByUserGroupedByExercise_nullUser() {
        final var result = this.analyticsService.getSessionsByUserGroupedByExercise(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @TestTransaction
    @DisplayName("getUsersProgressSummaryByUsernameSearch returns all for blank term")
    void testGetUsersProgressSummaryByUsernameSearch_blank() {
        final List<StudentProgressSummaryDto> summaries =
                this.analyticsService.getUsersProgressSummaryByUsernameSearch(CREDENTIALS, "");
        assertNotNull(summaries);
        assertFalse(summaries.isEmpty(), "Blank search delegates to getAllUsersProgressSummary");
    }

    @Test
    @TestTransaction
    @DisplayName("getUsersProgressSummaryByUsernameSearch returns matching entries")
    void testGetUsersProgressSummaryByUsernameSearch_match() {
        final List<StudentProgressSummaryDto> summaries =
                this.analyticsService.getUsersProgressSummaryByUsernameSearch(CREDENTIALS, "admin");
        assertNotNull(summaries);
        assertFalse(summaries.isEmpty(), "admin user should match");
        assertTrue(summaries.stream().anyMatch(s -> "admin".equals(s.username)));
    }

    @Test
    @TestTransaction
    @DisplayName("getUsersProgressSummaryByUsernameSearch returns empty for no match")
    void testGetUsersProgressSummaryByUsernameSearch_noMatch() {
        final List<StudentProgressSummaryDto> summaries =
                this.analyticsService.getUsersProgressSummaryByUsernameSearch(CREDENTIALS, "zzz_nobody_xyz");
        assertNotNull(summaries);
        assertTrue(summaries.isEmpty());
    }

    @Test
    @TestTransaction
    @DisplayName("getSessionsByDateRange with only startDate returns sessions after that date")
    void testGetSessionsByDateRange_onlyStart() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        final String sessionId = this.createSessionFor(studentId, exerciseId);

        final LocalDateTime start = LocalDateTime.now(ZoneId.systemDefault()).minusMinutes(1);
        final List<StudentSessionViewDto> sessions =
                this.analyticsService.getSessionsByDateRange(CREDENTIALS, start, null);
        assertNotNull(sessions);
        assertTrue(sessions.stream().anyMatch(s -> sessionId.equals(s.sessionId)),
                "Session should appear when filtering with only start date");
    }

    @Test
    @TestTransaction
    @DisplayName("getSessionsByDateRange with only endDate returns sessions before that date")
    void testGetSessionsByDateRange_onlyEnd() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        final String sessionId = this.createSessionFor(studentId, exerciseId);

        final LocalDateTime end = LocalDateTime.now(ZoneId.systemDefault()).plusMinutes(1);
        final List<StudentSessionViewDto> sessions =
                this.analyticsService.getSessionsByDateRange(CREDENTIALS, null, end);
        assertNotNull(sessions);
        assertTrue(sessions.stream().anyMatch(s -> sessionId.equals(s.sessionId)),
                "Session should appear when filtering with only end date");
    }

    @Test
    @TestTransaction
    @DisplayName("getUserCount returns a positive count")
    void testGetUserCount() {
        final long count = this.analyticsService.getUserCount();
        assertTrue(count >= 4, "Should have at least 4 seeded users");
    }

    @Test
    @TestTransaction
    @DisplayName("getPublishedExerciseCount returns a non-negative count")
    void testGetPublishedExerciseCount() {
        final long count = this.analyticsService.getPublishedExerciseCount();
        assertTrue(count >= 0, "Published exercise count should be non-negative");
        final Long exerciseId = this.createExercise();
        assertNotNull(exerciseId);
        final long after = this.analyticsService.getPublishedExerciseCount();
        assertTrue(after >= count, "Creating an exercise should not decrease published count");
    }

    @Test
    @TestTransaction
    @DisplayName("getDailySessionCounts returns a map with days+1 entries")
    void testGetDailySessionCounts() {
        final Map<LocalDate, Long> counts = this.analyticsService.getDailySessionCounts(7);
        assertNotNull(counts);
        assertEquals(8, counts.size(), "Should return 8 entries for 7 days (0..7 inclusive)");
        assertTrue(counts.containsKey(LocalDate.now(ZoneId.systemDefault())), "Should include today's date");
    }

    @Test
    @TestTransaction
    @DisplayName("getCompletionRateHistogram returns all six buckets")
    void testGetCompletionRateHistogram() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        final var session = this.createAndPersistSession(studentId, exerciseId);
        session.actionsCount = 10;
        session.correctActions = 5;

        final var hist = this.analyticsService.getCompletionRateHistogram();
        assertNotNull(hist);
        assertTrue(hist.containsKey("0%"));
        assertTrue(hist.containsKey("1-25%"));
        assertTrue(hist.containsKey("26-50%"));
        assertTrue(hist.containsKey("51-75%"));
        assertTrue(hist.containsKey("76-99%"));
        assertTrue(hist.containsKey("100%"));
    }

    @Test
    @TestTransaction
    @DisplayName("getHintUsageBuckets returns all four buckets")
    void testGetHintUsageBuckets() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        final var session = this.createAndPersistSession(studentId, exerciseId);
        session.hintsUsed = 2;

        final var buckets = this.analyticsService.getHintUsageBuckets();
        assertNotNull(buckets);
        assertTrue(buckets.containsKey("0 hints"));
        assertTrue(buckets.containsKey("1-3 hints"));
        assertTrue(buckets.containsKey("4-7 hints"));
        assertTrue(buckets.containsKey("8+ hints"));
    }

    @Test
    @TestTransaction
    @DisplayName("getRecentSessions returns sessions within the limit")
    void testGetRecentSessions() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        this.createSessionFor(studentId, exerciseId);

        final var recent = this.analyticsService.getRecentSessions(CREDENTIALS, 10);
        assertNotNull(recent);
        assertFalse(recent.isEmpty(), "Should find at least one recent session");
        assertTrue(recent.size() <= 10, "Should not exceed the limit");
    }

    @Test
    @TestTransaction
    @DisplayName("getTopStudentsByCompletion returns top students sorted by completion")
    void testGetTopStudentsByCompletion() {
        final var top = this.analyticsService.getTopStudentsByCompletion(CREDENTIALS, 5);
        assertNotNull(top);
        assertTrue(top.size() <= 5, "Should not exceed the limit");
        if (top.size() >= 2) {
            assertTrue(top.get(0).completedSessions >= top.get(1).completedSessions,
                    "Should be sorted descending by completed sessions");
        }
    }

    @Test
    @TestTransaction
    @DisplayName("getTrendData returns a non-null DTO with valid values")
    void testGetTrendData() {
        final var trend = this.analyticsService.getTrendData();
        assertNotNull(trend);
        assertTrue(trend.totalSessions >= 0);
        assertTrue(trend.completedSessions >= 0);
        assertTrue(trend.activeStudents >= 0);
        assertTrue(trend.todaySessions >= 0);
        assertTrue(trend.totalUsers >= 4);
    }

    @Test
    @TestTransaction
    @DisplayName("getUsersProgressSummaryByDateRange includes users active in range and excludes those outside")
    void testGetUsersProgressSummaryByDateRange() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        final var session = this.createAndPersistSession(studentId, exerciseId);
        session.startTime = LocalDateTime.now(ZoneId.systemDefault());

        final LocalDateTime start = LocalDateTime.now(ZoneId.systemDefault()).minusMinutes(1);
        final LocalDateTime end = LocalDateTime.now(ZoneId.systemDefault()).plusMinutes(1);
        final var summaries = this.analyticsService.getUsersProgressSummaryByDateRange(CREDENTIALS, start, end);
        assertNotNull(summaries);
        assertTrue(summaries.stream().anyMatch(s -> "student1".equals(s.username)),
                "student1 should appear in the date range summary");

        final LocalDateTime pastStart = LocalDateTime.now(ZoneId.systemDefault()).minusHours(2);
        final LocalDateTime pastEnd = LocalDateTime.now(ZoneId.systemDefault()).minusHours(1);
        final var pastSummaries =
                this.analyticsService.getUsersProgressSummaryByDateRange(CREDENTIALS, pastStart, pastEnd);
        assertNotNull(pastSummaries);
        assertFalse(pastSummaries.stream().anyMatch(s -> "student1".equals(s.username) && s.totalSessions > 0),
                "student1 should not appear active in an old range with no sessions");
    }

    @Test
    @TestTransaction
    @DisplayName("getProblemCategoryStats returns a non-null map")
    void testGetProblemCategoryStats() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        final ExerciseEntity exercise = this.exerciseRepository.findById(exerciseId);
        assertNotNull(exercise, "Created exercise must exist");
        final var session = this.createAndPersistSession(studentId, exerciseId);
        session.completed = true;
        session.actionsCount = 5;
        session.correctActions = 4;

        final var stats = this.analyticsService.getProblemCategoryStats();
        assertNotNull(stats);
        assertTrue(stats.containsKey(exercise.title), "Stats should contain the seeded exercise title");
        assertEquals(1, stats.get(exercise.title), "Should have exactly one completed session for the exercise");
    }

    @Test
    @DisplayName("Per-user reads require the administration view once per call, also when they delegate")
    @TestTransaction
    void shouldRequireAdminViewOncePerCall() {
        this.analyticsService.getAllSessions(CREDENTIALS);
        this.analyticsService.getUsersProgressSummaryByUsernameSearch(CREDENTIALS, "");

        verify(this.permissionService, times(2)).requireAdminView(CREDENTIALS);
    }

    @Test
    @DisplayName("Per-user reads propagate the refusal and return no data")
    @TestTransaction
    void shouldPropagateAdminViewRefusal() {
        doThrow(new PermissionDeniedException("view the administration")).when(this.permissionService)
                .requireAdminView(CREDENTIALS);

        assertThrows(PermissionDeniedException.class, () -> this.analyticsService.getAllSessions(CREDENTIALS));
        assertThrows(PermissionDeniedException.class,
                () -> this.analyticsService.getTopStudentsByCompletion(CREDENTIALS, 5));
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
    @DisplayName("The dashboard's anonymous counts need no credentials")
    @TestTransaction
    void shouldNotGuardAnonymousAggregates() {
        this.analyticsService.getDailySessionCounts(7);
        this.analyticsService.getTotalSessionsCount();

        verify(this.permissionService, never()).requireAdminView(any());
    }
}
