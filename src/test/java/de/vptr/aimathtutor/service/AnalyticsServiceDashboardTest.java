package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.StudentSessionEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserRankEntity;
import de.vptr.aimathtutor.repository.ExerciseRepository;
import de.vptr.aimathtutor.repository.StudentSessionRepository;
import de.vptr.aimathtutor.repository.UserRankRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@QuarkusTest
@SuppressWarnings("NullAway")
class AnalyticsServiceDashboardTest {

    @Inject
    AnalyticsService analyticsService;

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

    private void newSession(final UserEntity user, final ExerciseEntity ex, final LocalDateTime start,
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
    }

    /** The per-user mean success rate, computed in Java over all sessions with the operator's definition. */
    private double expectedAverageSuccessRate(final boolean countZeroActionUsers) {
        this.entityManager.flush();
        final List<StudentSessionEntity> all =
                this.entityManager.createQuery("SELECT s FROM StudentSessionEntity s WHERE s.user IS NOT NULL",
                        StudentSessionEntity.class).getResultList();
        final var byUser = all.stream().collect(Collectors.groupingBy(s -> s.user.id));
        return byUser.values().stream().mapToDouble(sessions -> {
            final double actions = sessions.stream().mapToInt(s -> s.actionsCount).sum();
            final double correct = sessions.stream().mapToInt(s -> s.correctActions).sum();
            return actions > 0 ? correct / actions : (countZeroActionUsers ? 0.0 : Double.NaN);
        }).filter(r -> !Double.isNaN(r)).average().orElse(0.0);
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
        assertEquals(this.expectedAverageSuccessRate(false), actual, 1e-9);
        assertTrue(Math.abs(this.expectedAverageSuccessRate(true) - actual) > 1e-9,
                "A user with only 0-action sessions must not pull the mean down");
    }

    @Test
    @TestTransaction
    @DisplayName("getDailySessionCounts counts sessions per start date and excludes those outside the window")
    void testGetDailySessionCountsSeeded() {
        final var user = this.newUser();
        final var ex = this.newExercise(user);
        final var zone = ZoneId.systemDefault();
        final var today = LocalDate.now(zone);
        final var before = this.analyticsService.getDailySessionCounts(7);

        final var twoDaysAgo = today.minusDays(2).atTime(12, 0);
        this.newSession(user, ex, twoDaysAgo, false, 0, 0);
        this.newSession(user, ex, twoDaysAgo.plusHours(1), false, 0, 0);
        this.newSession(user, ex, today.minusDays(4).atTime(9, 0), false, 0, 0);
        this.newSession(user, ex, today.minusDays(30).atTime(9, 0), false, 0, 0);

        final var after = this.analyticsService.getDailySessionCounts(7);
        assertEquals(before.get(today.minusDays(2)) + 2, after.get(today.minusDays(2)));
        assertEquals(before.get(today.minusDays(4)) + 1, after.get(today.minusDays(4)));
        assertEquals(8, after.size());
        assertTrue(!after.containsKey(today.minusDays(30)), "A session outside the window must not appear");
        assertEquals(before.values().stream().mapToLong(Long::longValue).sum() + 3,
                after.values().stream().mapToLong(Long::longValue).sum());
    }

    @Test
    @TestTransaction
    @DisplayName("getTrendData counts completed sessions of the current week only")
    void testGetTrendDataCompleted() {
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
}
