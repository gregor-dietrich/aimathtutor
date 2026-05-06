package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.AiInteractionViewDto;
import de.vptr.aimathtutor.dto.ExerciseDto;
import de.vptr.aimathtutor.dto.ExerciseViewDto;
import de.vptr.aimathtutor.dto.StudentProgressSummaryDto;
import de.vptr.aimathtutor.dto.StudentSessionViewDto;
import de.vptr.aimathtutor.repository.UserRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
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

    private Long studentId() {
        final var user = this.userRepository.findByUsername("student1");
        assertNotNull(user, "Seeded student1 must exist");
        return user.id;
    }

    private Long teacherId() {
        final var user = this.userRepository.findByUsername("teacher");
        assertNotNull(user, "Seeded teacher must exist");
        return user.id;
    }

    private Long createExercise() {
        final var teacher = this.userRepository.findByUsername("teacher");
        assertNotNull(teacher, "Seeded teacher must exist");
        final var dto = new ExerciseDto();
        final var suffix = UUID.randomUUID().toString().substring(0, 8);
        dto.title = "analytics_ex_" + suffix;
        dto.content = "content " + suffix;
        dto.userPublicId = teacher.publicId;
        dto.published = true;
        dto.commentable = false;
        final ExerciseViewDto created = this.exerciseService.createExercise(dto);
        return created.id;
    }

    @Test
    @TestTransaction
    @DisplayName("getAllSessions returns a non-null list")
    void testGetAllSessions() {
        final List<StudentSessionViewDto> sessions = this.analyticsService.getAllSessions();
        assertNotNull(sessions);
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
        this.graspableMathService.createSession(studentId, exerciseId);

        final List<StudentSessionViewDto> sessions =
                this.analyticsService.getSessionsByUserAndExercise(studentId, exerciseId);
        assertNotNull(sessions);
        assertFalse(sessions.isEmpty());
    }

    @Test
    @TestTransaction
    @DisplayName("getSessionsByDateRange with both bounds returns sessions within range")
    void testGetSessionsByDateRange_bothBounds() {
        final Long studentId = this.studentId();
        final Long exerciseId = this.createExercise();
        this.graspableMathService.createSession(studentId, exerciseId);

        final LocalDateTime start = LocalDateTime.now().minusMinutes(1);
        final LocalDateTime end = LocalDateTime.now().plusMinutes(1);
        final List<StudentSessionViewDto> sessions =
                this.analyticsService.getSessionsByDateRange(start, end);
        assertNotNull(sessions);
        assertFalse(sessions.isEmpty(), "Session created moments ago should be within range");
    }

    @Test
    @TestTransaction
    @DisplayName("getSessionsByDateRange with null bounds returns all sessions")
    void testGetSessionsByDateRange_nullBounds() {
        final List<StudentSessionViewDto> sessions =
                this.analyticsService.getSessionsByDateRange(null, null);
        assertNotNull(sessions);
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

        final StudentSessionViewDto found =
                this.analyticsService.getSessionBySessionId(sessionId);
        assertNotNull(found);
        assertEquals(sessionId, found.sessionId);
    }

    @Test
    @TestTransaction
    @DisplayName("getSessionBySessionId returns null for unknown session ID")
    void testGetSessionBySessionId_notFound() {
        final StudentSessionViewDto result =
                this.analyticsService.getSessionBySessionId("nonexistent-session-id");
        assertNull(result);
    }

    @Test
    @TestTransaction
    @DisplayName("getAllAiInteractions returns a non-null list")
    void testGetAllAiInteractions() {
        final List<AiInteractionViewDto> interactions = this.analyticsService.getAllAiInteractions();
        assertNotNull(interactions);
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
        final List<AiInteractionViewDto> interactions =
                this.analyticsService.getAiInteractionsByUser(this.studentId());
        assertNotNull(interactions);
    }

    @Test
    @TestTransaction
    @DisplayName("getUserProgressSummary returns null for non-existent user")
    void testGetUserProgressSummary_notFound() {
        final StudentProgressSummaryDto result =
                this.analyticsService.getUserProgressSummary(-999L);
        assertNull(result);
    }

    @Test
    @TestTransaction
    @DisplayName("getUserProgressSummary returns zero-session summary for user with no sessions")
    void testGetUserProgressSummary_noSessions() {
        final Long adminId = this.userRepository.findByUsername("admin").id;
        final StudentProgressSummaryDto summary =
                this.analyticsService.getUserProgressSummary(adminId);
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
        final Long exerciseId = this.createExercise();
        this.graspableMathService.createSession(studentId, exerciseId);

        final StudentProgressSummaryDto summary =
                this.analyticsService.getUserProgressSummary(studentId);
        assertNotNull(summary);
        assertEquals("student1", summary.username);
        assertTrue(summary.totalSessions >= 1);
        assertNotNull(summary.getCompletionRatePercentage());
        assertNotNull(summary.getSuccessRatePercentage());
        assertNotNull(summary.getFormattedAverageActions());
    }

    @Test
    @TestTransaction
    @DisplayName("getAllUsersProgressSummary returns at least one entry for seeded users")
    void testGetAllUsersProgressSummary() {
        final List<StudentProgressSummaryDto> summaries =
                this.analyticsService.getAllUsersProgressSummary();
        assertNotNull(summaries);
        assertFalse(summaries.isEmpty(), "Seeded users should produce at least one summary");
    }

    @Test
    @TestTransaction
    @DisplayName("getTotalSessionsCount returns a non-negative value")
    void testGetTotalSessionsCount() {
        assertTrue(this.analyticsService.getTotalSessionsCount() >= 0);
    }

    @Test
    @TestTransaction
    @DisplayName("getCompletedSessionsCount is at most totalSessionsCount")
    void testGetCompletedSessionsCount() {
        assertTrue(this.analyticsService.getCompletedSessionsCount()
                <= this.analyticsService.getTotalSessionsCount());
    }

    @Test
    @TestTransaction
    @DisplayName("getActiveStudentsCount returns a non-negative value")
    void testGetActiveStudentsCount() {
        assertTrue(this.analyticsService.getActiveStudentsCount() >= 0);
    }

    @Test
    @TestTransaction
    @DisplayName("getTodaySessionsCount is at most totalSessionsCount")
    void testGetTodaySessionsCount() {
        assertTrue(this.analyticsService.getTodaySessionsCount()
                <= this.analyticsService.getTotalSessionsCount());
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
        final List<StudentSessionViewDto> results =
                this.analyticsService.searchSessions("zzz_no_match_xyz_999");
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

        final List<StudentSessionViewDto> results =
                this.analyticsService.searchSessions("student1");
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Search by username should find student1 sessions");
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
    @DisplayName("getProblemCategoryStats returns a non-null map")
    void testGetProblemCategoryStats() {
        final var stats = this.analyticsService.getProblemCategoryStats();
        assertNotNull(stats);
    }

}
