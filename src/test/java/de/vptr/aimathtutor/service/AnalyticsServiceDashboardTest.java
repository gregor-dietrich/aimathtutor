package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.SessionCredentials;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.StudentSessionEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserRankEntity;
import de.vptr.aimathtutor.repository.ExerciseRepository;
import de.vptr.aimathtutor.repository.StudentSessionRepository;
import de.vptr.aimathtutor.repository.UserRankRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.security.PermissionService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@QuarkusTest
@SuppressWarnings("NullAway")
class AnalyticsServiceDashboardTest {

    // The mocked PermissionService accepts any credentials (the guard is tested in AnalyticsServiceAdminReadsTest)
    private static final SessionCredentials CREDENTIALS =
            new SessionCredentials("admin-public-id", "stamp", "session-key");

    @Inject
    AnalyticsService analyticsService;

    @InjectMock
    PermissionService permissionService;

    @Inject
    StudentSessionRepository studentSessionRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    UserRankRepository userRankRepository;

    @Inject
    ExerciseRepository exerciseRepository;

    @Inject
    EntityManager entityManager;

    private UserEntity newUser() {
        final var suffix = UUID.randomUUID().toString().substring(0, 8);
        final var rank = new UserRankEntity();
        rank.name = "Rank_" + suffix;
        this.userRankRepository.persist(rank);
        final var user = new UserEntity();
        user.username = "dash_" + suffix;
        user.password = "pw";
        user.email = "dash_" + suffix + "@example.com";
        user.activated = true;
        user.rank = rank;
        this.userRepository.persist(user);
        return user;
    }

    private ExerciseEntity newExercise(final UserEntity owner) {
        final var ex = new ExerciseEntity();
        ex.title = "dash_ex_" + UUID.randomUUID();
        ex.content = "x + 2";
        ex.user = owner;
        ex.published = true;
        this.exerciseRepository.persist(ex);
        return ex;
    }

    private StudentSessionEntity newSession(final UserEntity user, final ExerciseEntity ex, final LocalDateTime start,
            final boolean completed, final int actions, final int correct) {
        final var session = new StudentSessionEntity();
        session.sessionId = UUID.randomUUID().toString();
        session.user = user;
        session.exercise = ex;
        session.startTime = start;
        session.completed = completed;
        session.actionsCount = actions;
        session.correctActions = correct;
        this.studentSessionRepository.persist(session);
        return session;
    }

    private StudentSessionEntity newSession(final UserEntity user, final ExerciseEntity ex, final LocalDateTime start,
            final LocalDateTime end, final boolean completed, final int actions, final int correct, final int hints) {
        final var session = this.newSession(user, ex, start, completed, actions, correct);
        session.endTime = end;
        session.hintsUsed = hints;
        return session;
    }

    private List<StudentSessionEntity> allUserSessions() {
        this.entityManager.flush();
        return this.entityManager.createQuery("SELECT s FROM StudentSessionEntity s WHERE s.user IS NOT NULL",
                StudentSessionEntity.class).getResultList();
    }

    /**
     * The mean of per-user rates (correct / actions over each user's sessions), computed in Java over all sessions. The
     * dashboard counts only users with actions; {@code includeUsersWithoutActions} counts the others as 0.
     */
    private double meanOfUserRates(final boolean includeUsersWithoutActions) {
        return this.allUserSessions().stream().collect(Collectors.groupingBy(s -> s.user.id)).values().stream()
                .filter(sessions -> includeUsersWithoutActions || sessions.stream().anyMatch(s -> s.actionsCount > 0))
                .mapToDouble(sessions -> {
                    final double actions = sessions.stream().mapToInt(s -> s.actionsCount).sum();
                    final double correct = sessions.stream().mapToInt(s -> s.correctActions).sum();
                    return actions > 0 ? correct / actions : 0.0;
                }).average().orElse(0.0);
    }

    /** The pooled rate over all sessions (sum of correct / sum of actions), which the dashboard does not use. */
    private double pooledRate() {
        final var sessions = this.allUserSessions();
        final double actions = sessions.stream().mapToInt(s -> s.actionsCount).sum();
        return sessions.stream().mapToInt(s -> s.correctActions).sum() / actions;
    }

    @Test
    @TestTransaction
    @DisplayName("getAverageSuccessRate is the mean of per-user rates and ignores users without actions")
    void testGetAverageSuccessRate() {
        final var a = this.newUser();
        final var b = this.newUser();
        final var idle = this.newUser();
        final var ex = this.newExercise(a);
        final var now = LocalDateTime.now(ZoneId.systemDefault());
        this.newSession(a, ex, now, true, 2, 1);
        this.newSession(b, ex, now, true, 3, 3);
        this.newSession(b, ex, now, false, 1, 0);
        this.newSession(idle, ex, now, false, 0, 0);
        this.newSession(idle, ex, now, false, 0, 0);

        final double actual = this.analyticsService.getAverageSuccessRate();

        // The seeded demo sessions also count, so compare against the same definition computed in Java
        assertEquals(this.meanOfUserRates(false), actual, 1e-9);
        assertTrue(Math.abs(this.meanOfUserRates(true) - actual) > 1e-9,
                "A user with only 0-action sessions must not pull the mean down");
        assertTrue(Math.abs(this.pooledRate() - actual) > 1e-9, "Each user counts once, however many actions");
    }

    @Test
    @TestTransaction
    @DisplayName("getDailySessionCounts counts whole days per start date and excludes those outside the window")
    void testGetDailySessionCounts_seeded() {
        final var user = this.newUser();
        final var ex = this.newExercise(user);
        final var zone = ZoneId.systemDefault();
        final var today = LocalDate.now(zone);
        final var before = this.analyticsService.getDailySessionCounts(7);

        final var twoDaysAgo = today.minusDays(2).atTime(12, 0);
        this.newSession(user, ex, twoDaysAgo, false, 0, 0);
        this.newSession(user, ex, twoDaysAgo.plusHours(1), false, 0, 0);
        this.newSession(user, ex, today.minusDays(4).atTime(9, 0), false, 0, 0);
        this.newSession(user, ex, today.minusDays(7).atStartOfDay(), false, 0, 0);
        this.newSession(user, ex, today.minusDays(30).atTime(9, 0), false, 0, 0);

        final var after = this.analyticsService.getDailySessionCounts(7);
        assertEquals(before.get(today.minusDays(2)) + 2, after.get(today.minusDays(2)));
        assertEquals(before.get(today.minusDays(4)) + 1, after.get(today.minusDays(4)));
        assertEquals(before.get(today.minusDays(7)) + 1, after.get(today.minusDays(7)),
                "The first day counts from its start, not from the current time of day");
        assertFalse(after.containsKey(today.minusDays(30)), "A session outside the window must not appear");
        assertEquals(before.values().stream().mapToLong(Long::longValue).sum() + 4,
                after.values().stream().mapToLong(Long::longValue).sum());
    }

    @Test
    @TestTransaction
    @DisplayName("getTrendData counts completed sessions of the current week only")
    void testGetTrendData_completed() {
        final var user = this.newUser();
        final var ex = this.newExercise(user);
        final var now = LocalDateTime.now(ZoneId.systemDefault());
        final var before = this.analyticsService.getTrendData();

        this.newSession(user, ex, now.minusDays(1), true, 0, 0);
        this.newSession(user, ex, now.minusDays(2), true, 0, 0);
        this.newSession(user, ex, now.minusDays(3), false, 0, 0);
        this.newSession(user, ex, now.minusDays(10), true, 0, 0);

        final var after = this.analyticsService.getTrendData();
        assertEquals(before.completedSessions + 2, after.completedSessions);
        assertEquals(before.prevCompletedSessions + 1, after.prevCompletedSessions);
        assertEquals(before.totalSessions + 3, after.totalSessions);
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
    @DisplayName("getDailySessionCounts returns a map with days+1 entries")
    void testGetDailySessionCounts() {
        final Map<LocalDate, Long> counts = this.analyticsService.getDailySessionCounts(7);
        assertNotNull(counts);
        assertEquals(8, counts.size(), "Should return 8 entries for 7 days (0..7 inclusive)");
        assertTrue(counts.containsKey(LocalDate.now(ZoneId.systemDefault())), "Should include today's date");
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
    @DisplayName("getTopStudentsByCompletion totals the top user's sessions in SQL, with every field as before")
    void testGetTopStudentsByCompletion() {
        final var user = this.newUser();
        final var ex = this.newExercise(user);
        final var base = LocalDateTime.of(2030, 1, 1, 8, 0);
        // 50 completed sessions: 2 actions (1 correct) and 1 hint each, each ended an hour after its start
        for (int i = 0; i < 50; i++) {
            final var start = base.plusDays(i);
            this.newSession(user, ex, start, start.plusHours(1), true, 2, 1, 1);
        }
        // Incomplete, no end time, started later than every end time: lastActivity falls back to its start time
        final var lastStart = base.plusDays(60);
        this.newSession(user, ex, lastStart, false, 3, 2);
        this.entityManager.flush();

        final var top = this.analyticsService.getTopStudentsByCompletion(CREDENTIALS, 3);

        final var summary = top.get(0);
        assertEquals(user.publicId, summary.userPublicId);
        assertEquals(user.username, summary.username);
        assertEquals(51, summary.totalSessions.intValue());
        assertEquals(50, summary.completedSessions.intValue());
        assertEquals(51, summary.totalProblems.intValue());
        assertEquals(50, summary.completedProblems.intValue());
        assertEquals(50, summary.hintsUsed.intValue());
        assertEquals(103.0 / 51, summary.averageActionsPerProblem, 1e-9);
        assertEquals(52.0 / 103, summary.successRate, 1e-9);
        assertEquals(lastStart, summary.lastActivity);

        // An end time later than the null-end session's start wins
        this.newSession(user, ex, base, lastStart.plusDays(1), true, 0, 0, 0);
        this.entityManager.flush();
        assertEquals(lastStart.plusDays(1),
                this.analyticsService.getTopStudentsByCompletion(CREDENTIALS, 1).get(0).lastActivity);
    }
}
