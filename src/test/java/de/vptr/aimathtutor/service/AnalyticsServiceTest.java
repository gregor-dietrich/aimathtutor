package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import de.vptr.aimathtutor.dto.StudentProgressSummaryDto;
import de.vptr.aimathtutor.dto.StudentSessionViewDto;
import de.vptr.aimathtutor.entity.AiInteractionEntity;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.StudentSessionEntity;
import de.vptr.aimathtutor.repository.AiInteractionRepository;
import de.vptr.aimathtutor.repository.ExerciseRepository;
import de.vptr.aimathtutor.repository.StudentSessionRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.security.AuthService;
import de.vptr.aimathtutor.service.security.PermissionService;
import de.vptr.aimathtutor.util.TestExerciseFactory;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import static org.mockito.Mockito.when;

@QuarkusTest
@SuppressWarnings({ "NullAway", "PMD.TooManyMethods" })
class AnalyticsServiceTest {

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
    AiInteractionRepository aiInteractionRepository;

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

    private String createSessionWithInteraction(final Long studentId, final Long exerciseId, final String eventType,
            final String feedbackType) {
        final String sessionId = this.graspableMathService.createSession(studentId, exerciseId);
        final var interaction = new AiInteractionEntity();
        interaction.sessionId = sessionId;
        final var userForInteraction = this.userRepository.findById(studentId);
        assertNotNull(userForInteraction, "Seeded student1 must exist");
        interaction.user = userForInteraction;
        final var exercise = this.exerciseRepository.findById(exerciseId);
        assertNotNull(exercise, "Created exercise must exist");
        interaction.exercise = exercise;
        interaction.eventType = eventType;
        interaction.feedbackType = feedbackType;
        this.aiInteractionRepository.persist(interaction);
        return sessionId;
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

    private record SessionFixture(Long studentId, Long exerciseId, String sessionId) {
    }

    private SessionFixture createSessionFixture() {
        final Long studentId = this.studentId();
        final Long exerciseId = TestExerciseFactory.createExercise(this.userRepository, this.exerciseService).id;
        final String sessionId = this.graspableMathService.createSession(studentId, exerciseId);
        return new SessionFixture(studentId, exerciseId, sessionId);
    }

    private void assertDateRangeFilter(final String sessionId, final List<? extends StudentSessionViewDto> within,
            final List<? extends StudentSessionViewDto> outside) {
        assertNotNull(within);
        assertTrue(within.stream().anyMatch(s -> sessionId.equals(s.sessionId)),
                "Session should appear within the date range");
        assertNotNull(outside);
        assertFalse(outside.stream().anyMatch(s -> sessionId.equals(s.sessionId)),
                "Session should not appear in a future range");
    }

    @Test
    @TestTransaction
    @DisplayName("getAllSessions returns a non-null list")
    void testGetAllSessions() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        final String sessionId = this.graspableMathService.createSession(studentId, exerciseId);

        final List<StudentSessionViewDto> sessions = this.analyticsService.getAllSessions();
        assertNotNull(sessions);
        assertTrue(sessions.stream().anyMatch(s -> sessionId.equals(s.sessionId)),
                "Seeded session should appear in the list");
    }

    @Test
    @TestTransaction
    @DisplayName("getSessionsByUser returns sessions for the given user")
    void testGetSessionsByUser() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        this.graspableMathService.createSession(studentId, exerciseId);

        final List<StudentSessionViewDto> sessions = this.analyticsService.getSessionsByUser(studentId);
        assertNotNull(sessions);
        assertFalse(sessions.isEmpty(), "Should find at least one session for student1");
        assertTrue(sessions.stream().allMatch(s -> "student1".equals(s.username)),
                "All returned sessions should belong to student1");
    }

    @Test
    @TestTransaction
    @DisplayName("getSessionsByExercise returns sessions for the given exercise")
    void testGetSessionsByExercise() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        this.graspableMathService.createSession(studentId, exerciseId);

        final ExerciseViewDto exercise = this.exerciseService.findById(exerciseId).orElseThrow();
        final List<StudentSessionViewDto> sessions = this.analyticsService.getSessionsByExercise(exerciseId);
        assertNotNull(sessions);
        assertFalse(sessions.isEmpty(), "Should find at least one session for the exercise");
        assertTrue(sessions.stream().allMatch(s -> exercise.publicId.equals(s.exercisePublicId)),
                "All returned sessions should belong to the exercise");
    }

    @Test
    @TestTransaction
    @DisplayName("getSessionsByUserAndExercise filters by both user and exercise")
    void testGetSessionsByUserAndExercise() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        final String sessionId = this.graspableMathService.createSession(studentId, exerciseId);

        final var otherStudent = this.userRepository.findByUsername("student2");
        assertNotNull(otherStudent, "Seeded student2 must exist");
        final Long otherStudentId = otherStudent.id;
        final Long otherExerciseId = this.createExercise();
        final String otherSessionId = this.graspableMathService.createSession(otherStudentId, otherExerciseId);

        final Long differentExerciseId = this.createExercise();
        final String sameUserDiffExerciseSessionId =
                this.graspableMathService.createSession(studentId, differentExerciseId);

        final String diffUserSameExerciseSessionId =
                this.graspableMathService.createSession(otherStudentId, exerciseId);

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
        final String sessionId = this.graspableMathService.createSession(studentId, exerciseId);

        final LocalDateTime start = LocalDateTime.now(ZoneId.systemDefault()).minusMinutes(1);
        final LocalDateTime end = LocalDateTime.now(ZoneId.systemDefault()).plusMinutes(1);
        final List<StudentSessionViewDto> sessions = this.analyticsService.getSessionsByDateRange(start, end);
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
        final String sessionId = this.graspableMathService.createSession(studentId, exerciseId);

        final List<StudentSessionViewDto> sessions = this.analyticsService.getSessionsByDateRange(null, null);
        assertNotNull(sessions);
        assertTrue(sessions.stream().anyMatch(s -> sessionId.equals(s.sessionId)),
                "Seeded session should appear when bounds are null");
    }

    @Test
    @TestTransaction
    @DisplayName("getSessionById returns null for unknown ID")
    void testGetSessionById_notFound() {
        final StudentSessionViewDto result = this.analyticsService.getSessionById(-999L);
        assertNull(result);
    }

    @Test
    @TestTransaction
    @DisplayName("getSessionBySessionId returns the session matching the string ID")
    void testGetSessionBySessionId() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        final String sessionId = this.graspableMathService.createSession(studentId, exerciseId);

        final StudentSessionViewDto found = this.analyticsService.getSessionBySessionId(sessionId);
        assertNotNull(found);
        assertEquals(sessionId, found.sessionId);
    }

    @Test
    @TestTransaction
    @DisplayName("getSessionBySessionId returns null for unknown session ID")
    void testGetSessionBySessionId_notFound() {
        final StudentSessionViewDto result = this.analyticsService.getSessionBySessionId("nonexistent-session-id");
        assertNull(result);
    }

    @Test
    @TestTransaction
    @DisplayName("getAllAiInteractions returns a non-null list")
    void testGetAllAiInteractions() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        final String sessionId = this.createSessionWithInteraction(studentId, exerciseId, "test_event", "HINT");

        final List<AiInteractionViewDto> interactions = this.analyticsService.getAllAiInteractions();
        assertNotNull(interactions);
        assertTrue(interactions.stream().anyMatch(i -> sessionId.equals(i.sessionId)),
                "Seeded interaction should appear in the list");
    }

    @Test
    @TestTransaction
    @DisplayName("getAiInteractionsBySession returns empty list for unknown session")
    void testGetAiInteractionsBySession_notFound() {
        final List<AiInteractionViewDto> interactions =
                this.analyticsService.getAiInteractionsBySession("no-such-session");
        assertNotNull(interactions);
        assertTrue(interactions.isEmpty());
    }

    @Test
    @TestTransaction
    @DisplayName("getAiInteractionsByUser returns a non-null list")
    void testGetAiInteractionsByUser() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        final String sessionId =
                this.createSessionWithInteraction(studentId, exerciseId, "test_event_user", "POSITIVE");

        final List<AiInteractionViewDto> interactions = this.analyticsService.getAiInteractionsByUser(studentId);
        assertNotNull(interactions);
        assertTrue(interactions.stream().anyMatch(i -> sessionId.equals(i.sessionId)),
                "Seeded interaction should appear for the user");
    }

    @Test
    @TestTransaction
    @DisplayName("getAiInteractionsByUser filters out other users' interactions")
    void testGetAiInteractionsByUser_filtersByUser() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        final String sessionId =
                this.createSessionWithInteraction(studentId, exerciseId, "test_event_user_filter", "NEGATIVE");

        final var otherStudent = this.userRepository.findByUsername("student2");
        assertNotNull(otherStudent, "Seeded student2 must exist");
        final Long otherStudentId = otherStudent.id;
        final Long otherExerciseId = this.createExercise();
        final String otherSessionId = this.graspableMathService.createSession(otherStudentId, otherExerciseId);

        final var otherInteraction = new AiInteractionEntity();
        otherInteraction.sessionId = otherSessionId;
        otherInteraction.user = otherStudent;
        otherInteraction.eventType = "test_event_other_user";
        otherInteraction.feedbackType = "HINT";
        this.aiInteractionRepository.persist(otherInteraction);

        final List<AiInteractionViewDto> interactions = this.analyticsService.getAiInteractionsByUser(studentId);
        assertNotNull(interactions);
        assertTrue(interactions.stream().anyMatch(i -> sessionId.equals(i.sessionId)),
                "Seeded interaction should appear for the user");
        assertFalse(interactions.stream().anyMatch(i -> otherSessionId.equals(i.sessionId)),
                "Interactions from other users should not appear");
    }

    @Test
    @TestTransaction
    @DisplayName("getUserProgressSummary returns null for non-existent user")
    void testGetUserProgressSummary_notFound() {
        final StudentProgressSummaryDto result = this.analyticsService.getUserProgressSummary(-999L);
        assertNull(result);
    }

    @Test
    @TestTransaction
    @DisplayName("getUserProgressSummary returns zero-session summary for user with no sessions")
    void testGetUserProgressSummary_noSessions() {
        final var admin = this.userRepository.findByUsername("admin");
        assertNotNull(admin, "Seeded admin must exist");
        final Long adminId = admin.id;
        final StudentProgressSummaryDto summary = this.analyticsService.getUserProgressSummary(adminId);
        assertNotNull(summary);
        assertEquals("admin", summary.username);
        assertEquals(0, summary.totalSessions);
        assertEquals(0.0, summary.successRate);
    }

    @Test
    @TestTransaction
    @DisplayName("getUserProgressSummary aggregates session data correctly")
    void testGetUserProgressSummary_withSession() {
        final Long studentId = this.studentId();
        final StudentProgressSummaryDto baseline = this.analyticsService.getUserProgressSummary(studentId);
        assertNotNull(baseline);
        assertEquals("student1", baseline.username);
        final int baselineTotal = baseline.totalSessions;

        final Long exerciseId = this.createExercise();
        this.graspableMathService.createSession(studentId, exerciseId);

        final StudentProgressSummaryDto summary = this.analyticsService.getUserProgressSummary(studentId);
        assertNotNull(summary);
        assertEquals("student1", summary.username);
        assertEquals(baselineTotal + 1, summary.totalSessions,
                "Creating one session should increase totalSessions by 1");
        assertNotNull(summary.getCompletionRatePercentage());
        assertNotNull(summary.getSuccessRatePercentage());
        assertNotNull(summary.getFormattedAverageActions());
    }

    @Test
    @TestTransaction
    @DisplayName("getAllUsersProgressSummary returns at least one entry for seeded users")
    void testGetAllUsersProgressSummary() {
        final List<StudentProgressSummaryDto> summaries = this.analyticsService.getAllUsersProgressSummary();
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
        final List<StudentSessionViewDto> results = this.analyticsService.searchSessions("");
        assertNotNull(results);
        assertTrue(results.isEmpty(), "Blank search term should return empty list");
    }

    @Test
    @TestTransaction
    @DisplayName("searchSessions returns empty list for nonsense term")
    void testSearchSessions_noMatch() {
        final List<StudentSessionViewDto> results = this.analyticsService.searchSessions("zzz_no_match_xyz_999");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @TestTransaction
    @DisplayName("searchSessions returns matching sessions by username")
    void testSearchSessions_byUsername() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        this.graspableMathService.createSession(studentId, exerciseId);

        final var otherStudent = this.userRepository.findByUsername("student2");
        assertNotNull(otherStudent, "Seeded student2 must exist");
        final Long otherStudentId = otherStudent.id;
        final Long otherExerciseId = this.createExercise();
        this.graspableMathService.createSession(otherStudentId, otherExerciseId);

        final List<StudentSessionViewDto> results = this.analyticsService.searchSessions("student1");
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
                this.analyticsService.getUsersProgressSummaryByUsernameSearch("");
        assertNotNull(summaries);
        assertFalse(summaries.isEmpty(), "Blank search delegates to getAllUsersProgressSummary");
    }

    @Test
    @TestTransaction
    @DisplayName("getUsersProgressSummaryByUsernameSearch returns matching entries")
    void testGetUsersProgressSummaryByUsernameSearch_match() {
        final List<StudentProgressSummaryDto> summaries =
                this.analyticsService.getUsersProgressSummaryByUsernameSearch("admin");
        assertNotNull(summaries);
        assertFalse(summaries.isEmpty(), "admin user should match");
        assertTrue(summaries.stream().anyMatch(s -> "admin".equals(s.username)));
    }

    @Test
    @TestTransaction
    @DisplayName("getUsersProgressSummaryByUsernameSearch returns empty for no match")
    void testGetUsersProgressSummaryByUsernameSearch_noMatch() {
        final List<StudentProgressSummaryDto> summaries =
                this.analyticsService.getUsersProgressSummaryByUsernameSearch("zzz_nobody_xyz");
        assertNotNull(summaries);
        assertTrue(summaries.isEmpty());
    }

    @Test
    @TestTransaction
    @DisplayName("getSessionsByUserAndDateRange returns session within range and excludes it outside")
    void testGetSessionsByUserAndDateRange() {
        final var fixture = this.createSessionFixture();

        final LocalDateTime start = LocalDateTime.now(ZoneId.systemDefault()).minusMinutes(1);
        final LocalDateTime end = LocalDateTime.now(ZoneId.systemDefault()).plusMinutes(1);
        final var within = this.analyticsService.getSessionsByUserAndDateRange(fixture.studentId(), start, end);

        final LocalDateTime futureStart = LocalDateTime.now(ZoneId.systemDefault()).plusHours(1);
        final LocalDateTime futureEnd = LocalDateTime.now(ZoneId.systemDefault()).plusHours(2);
        final var outside =
                this.analyticsService.getSessionsByUserAndDateRange(fixture.studentId(), futureStart, futureEnd);
        this.assertDateRangeFilter(fixture.sessionId(), within, outside);
    }

    @Test
    @TestTransaction
    @DisplayName("getSessionsByExerciseAndDateRange returns session within range and excludes it outside")
    void testGetSessionsByExerciseAndDateRange() {
        final var fixture = this.createSessionFixture();

        final LocalDateTime start = LocalDateTime.now(ZoneId.systemDefault()).minusMinutes(1);
        final LocalDateTime end = LocalDateTime.now(ZoneId.systemDefault()).plusMinutes(1);
        final var within = this.analyticsService.getSessionsByExerciseAndDateRange(fixture.exerciseId(), start, end);

        final LocalDateTime futureStart = LocalDateTime.now(ZoneId.systemDefault()).plusHours(1);
        final LocalDateTime futureEnd = LocalDateTime.now(ZoneId.systemDefault()).plusHours(2);
        final var outside =
                this.analyticsService.getSessionsByExerciseAndDateRange(fixture.exerciseId(), futureStart, futureEnd);
        this.assertDateRangeFilter(fixture.sessionId(), within, outside);
    }

    @Test
    @TestTransaction
    @DisplayName("getSessionsByStatusAndDateRange filters completed vs incomplete sessions")
    void testGetSessionsByStatusAndDateRange() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        final String sessionId = this.graspableMathService.createSession(studentId, exerciseId);
        this.graspableMathService.markSessionComplete(sessionId);

        final LocalDateTime start = LocalDateTime.now(ZoneId.systemDefault()).minusMinutes(1);
        final LocalDateTime end = LocalDateTime.now(ZoneId.systemDefault()).plusMinutes(1);

        final var completed = this.analyticsService.getSessionsByStatusAndDateRange(Boolean.TRUE, start, end);
        assertNotNull(completed);
        assertTrue(completed.stream().anyMatch(s -> sessionId.equals(s.sessionId)),
                "Completed session should appear when filtering completed=true");

        final var incomplete = this.analyticsService.getSessionsByStatusAndDateRange(Boolean.FALSE, start, end);
        assertNotNull(incomplete);
        assertFalse(incomplete.stream().anyMatch(s -> sessionId.equals(s.sessionId)),
                "Completed session should not appear when filtering completed=false");
    }

    @Test
    @TestTransaction
    @DisplayName("getAiInteractionsByExercise returns interactions for that exercise")
    void testGetAiInteractionsByExercise() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        final String sessionId = this.graspableMathService.createSession(studentId, exerciseId);

        final var exercise = this.exerciseRepository.findById(exerciseId);
        assertNotNull(exercise, "Created exercise must exist");
        final var user = this.userRepository.findById(studentId);
        assertNotNull(user, "Seeded student1 must exist");

        final var interaction = new AiInteractionEntity();
        interaction.sessionId = sessionId;
        interaction.user = user;
        interaction.exercise = exercise;
        interaction.eventType = "test_exercise_interaction";
        interaction.feedbackType = "POSITIVE";
        this.aiInteractionRepository.persist(interaction);

        final Long otherExerciseId = this.createExercise();
        final var otherExercise = this.exerciseRepository.findById(otherExerciseId);
        final String otherSessionId = this.graspableMathService.createSession(studentId, otherExerciseId);
        final var otherInteraction = new AiInteractionEntity();
        otherInteraction.sessionId = otherSessionId;
        otherInteraction.user = user;
        otherInteraction.exercise = otherExercise;
        otherInteraction.eventType = "other_exercise_interaction";
        otherInteraction.feedbackType = "HINT";
        this.aiInteractionRepository.persist(otherInteraction);

        final List<AiInteractionViewDto> results = this.analyticsService.getAiInteractionsByExercise(exerciseId);
        assertNotNull(results);
        assertTrue(results.stream().anyMatch(i -> sessionId.equals(i.sessionId)),
                "Interaction for the exercise should appear");
        assertFalse(results.stream().anyMatch(i -> otherSessionId.equals(i.sessionId)),
                "Interaction from a different exercise should not appear");
    }

    @Test
    @TestTransaction
    @DisplayName("getSessionsByDateRange with only startDate returns sessions after that date")
    void testGetSessionsByDateRange_onlyStart() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        final String sessionId = this.graspableMathService.createSession(studentId, exerciseId);

        final LocalDateTime start = LocalDateTime.now(ZoneId.systemDefault()).minusMinutes(1);
        final List<StudentSessionViewDto> sessions = this.analyticsService.getSessionsByDateRange(start, null);
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
        final String sessionId = this.graspableMathService.createSession(studentId, exerciseId);

        final LocalDateTime end = LocalDateTime.now(ZoneId.systemDefault()).plusMinutes(1);
        final List<StudentSessionViewDto> sessions = this.analyticsService.getSessionsByDateRange(null, end);
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
        this.graspableMathService.createSession(studentId, exerciseId);

        final var recent = this.analyticsService.getRecentSessions(10);
        assertNotNull(recent);
        assertFalse(recent.isEmpty(), "Should find at least one recent session");
        assertTrue(recent.size() <= 10, "Should not exceed the limit");
    }

    @Test
    @TestTransaction
    @DisplayName("getTopStudentsByCompletion returns top students sorted by completion")
    void testGetTopStudentsByCompletion() {
        final var top = this.analyticsService.getTopStudentsByCompletion(5);
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
        final var summaries = this.analyticsService.getUsersProgressSummaryByDateRange(start, end);
        assertNotNull(summaries);
        assertTrue(summaries.stream().anyMatch(s -> "student1".equals(s.username)),
                "student1 should appear in the date range summary");

        final LocalDateTime pastStart = LocalDateTime.now(ZoneId.systemDefault()).minusHours(2);
        final LocalDateTime pastEnd = LocalDateTime.now(ZoneId.systemDefault()).minusHours(1);
        final var pastSummaries = this.analyticsService.getUsersProgressSummaryByDateRange(pastStart, pastEnd);
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

}
