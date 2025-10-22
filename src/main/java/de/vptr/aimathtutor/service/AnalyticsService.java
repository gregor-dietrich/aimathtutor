package de.vptr.aimathtutor.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.vptr.aimathtutor.dto.AIInteractionViewDto;
import de.vptr.aimathtutor.dto.StudentProgressSummaryDto;
import de.vptr.aimathtutor.dto.StudentSessionViewDto;
import de.vptr.aimathtutor.entity.AIInteractionEntity;
import de.vptr.aimathtutor.entity.StudentSessionEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Service for retrieving analytics and progress tracking data.
 * Used by admin views to display student sessions, AI interactions, and
 * progress summaries.
 */
@ApplicationScoped
public class AnalyticsService {

    private static final Logger LOG = LoggerFactory.getLogger(AnalyticsService.class);

    /**
     * Get all student sessions
     */
    @Transactional
    public List<StudentSessionViewDto> getAllSessions() {
        LOG.trace("Getting all student sessions");
        final List<StudentSessionEntity> sessions = StudentSessionEntity.listAll();
        return sessions.stream()
                .map(StudentSessionViewDto::new)
                .toList();
    }

    /**
     * Get sessions by user ID
     */
    @Transactional
    public List<StudentSessionViewDto> getSessionsByUser(final Long userId) {
        LOG.trace("Getting sessions for user: {}", userId);
        final List<StudentSessionEntity> sessions = StudentSessionEntity.find("user.id = ?1", userId).list();
        return sessions.stream()
                .map(StudentSessionViewDto::new)
                .toList();
    }

    /**
     * Get sessions by exercise ID
     */
    @Transactional
    public List<StudentSessionViewDto> getSessionsByExercise(final Long exerciseId) {
        LOG.trace("Getting sessions for exercise: {}", exerciseId);
        final List<StudentSessionEntity> sessions = StudentSessionEntity.find("exercise.id = ?1", exerciseId)
                .list();
        return sessions.stream()
                .map(StudentSessionViewDto::new)
                .toList();
    }

    /**
     * Get sessions by user and exercise ID
     * Efficient single-database query for filtering sessions by both user and
     * exercise
     */
    @Transactional
    public List<StudentSessionViewDto> getSessionsByUserAndExercise(final Long userId, final Long exerciseId) {
        LOG.trace("Getting sessions for user: {} on exercise: {}", userId, exerciseId);
        final List<StudentSessionEntity> sessions = StudentSessionEntity
                .find("user.id = ?1 AND exercise.id = ?2", userId, exerciseId)
                .list();
        return sessions.stream()
                .map(StudentSessionViewDto::new)
                .toList();
    }

    /**
     * Get sessions by user and date range
     */
    @Transactional
    public List<StudentSessionViewDto> getSessionsByUserAndDateRange(
            final Long userId,
            final LocalDateTime startDate,
            final LocalDateTime endDate) {
        LOG.trace("Getting sessions for user: {} between {} and {}", userId, startDate, endDate);
        final List<StudentSessionEntity> sessions = StudentSessionEntity
                .find("user.id = ?1 AND startTime >= ?2 AND startTime <= ?3",
                        userId, startDate, endDate)
                .list();
        return sessions.stream()
                .map(StudentSessionViewDto::new)
                .toList();
    }

    /**
     * Get sessions by exercise and date range
     */
    @Transactional
    public List<StudentSessionViewDto> getSessionsByExerciseAndDateRange(
            final Long exerciseId,
            final LocalDateTime startDate,
            final LocalDateTime endDate) {
        LOG.trace("Getting sessions for exercise: {} between {} and {}", exerciseId, startDate, endDate);
        final List<StudentSessionEntity> sessions = StudentSessionEntity
                .find("exercise.id = ?1 AND startTime >= ?2 AND startTime <= ?3",
                        exerciseId, startDate, endDate)
                .list();
        return sessions.stream()
                .map(StudentSessionViewDto::new)
                .toList();
    }

    /**
     * Get sessions by completion status and date range
     */
    @Transactional
    public List<StudentSessionViewDto> getSessionsByStatusAndDateRange(
            final Boolean completed,
            final LocalDateTime startDate,
            final LocalDateTime endDate) {
        LOG.trace("Getting {} sessions between {} and {}",
                completed ? "completed" : "incomplete", startDate, endDate);
        final List<StudentSessionEntity> sessions = StudentSessionEntity
                .find("completed = ?1 AND startTime >= ?2 AND startTime <= ?3",
                        completed, startDate, endDate)
                .list();
        return sessions.stream()
                .map(StudentSessionViewDto::new)
                .toList();
    }

    /**
     * Get a specific session by ID
     */
    @Transactional
    public StudentSessionViewDto getSessionById(final Long sessionId) {
        LOG.trace("Getting session: {}", sessionId);
        final StudentSessionEntity session = StudentSessionEntity.findById(sessionId);
        return session != null ? new StudentSessionViewDto(session) : null;
    }

    /**
     * Get a specific session by session ID string
     */
    @Transactional
    public StudentSessionViewDto getSessionBySessionId(final String sessionId) {
        LOG.trace("Getting session by session ID: {}", sessionId);
        final StudentSessionEntity session = StudentSessionEntity.findBySessionId(sessionId);
        return session != null ? new StudentSessionViewDto(session) : null;
    }

    /**
     * Get all AI interactions
     */
    @Transactional
    public List<AIInteractionViewDto> getAllAIInteractions() {
        LOG.trace("Getting all AI interactions");
        final List<AIInteractionEntity> interactions = AIInteractionEntity.listAll();
        return interactions.stream()
                .map(AIInteractionViewDto::new)
                .toList();
    }

    /**
     * Get AI interactions by session ID
     */
    @Transactional
    public List<AIInteractionViewDto> getAIInteractionsBySession(final String sessionId) {
        LOG.trace("Getting AI interactions for session: {}", sessionId);
        final List<AIInteractionEntity> interactions = AIInteractionEntity.find("sessionId = ?1", sessionId)
                .list();
        return interactions.stream()
                .map(AIInteractionViewDto::new)
                .toList();
    }

    /**
     * Get AI interactions by user ID
     */
    @Transactional
    public List<AIInteractionViewDto> getAIInteractionsByUser(final Long userId) {
        LOG.trace("Getting AI interactions for user: {}", userId);
        final List<AIInteractionEntity> interactions = AIInteractionEntity.find("user.id = ?1", userId).list();
        return interactions.stream()
                .map(AIInteractionViewDto::new)
                .toList();
    }

    /**
     * Get AI interactions by exercise ID
     */
    @Transactional
    public List<AIInteractionViewDto> getAIInteractionsByExercise(final Long exerciseId) {
        LOG.trace("Getting AI interactions for exercise: {}", exerciseId);
        final List<AIInteractionEntity> interactions = AIInteractionEntity.find("exercise.id = ?1", exerciseId)
                .list();
        return interactions.stream()
                .map(AIInteractionViewDto::new)
                .toList();
    }

    /**
     * Get progress summary for a single user
     */
    @Transactional
    public StudentProgressSummaryDto getUserProgressSummary(final Long userId) {
        LOG.trace("Getting progress summary for user: {}", userId);

        final UserEntity user = UserEntity.findById(userId);
        if (user == null) {
            return null;
        }

        final List<StudentSessionEntity> sessions = StudentSessionEntity.find("user.id = ?1", userId).list();
        return this.computeProgressSummary(user, sessions);
    }

    /**
     * Get progress summaries for all users
     * Refactored to avoid N+1 queries by fetching all sessions once and grouping by
     * user
     */
    @Transactional
    public List<StudentProgressSummaryDto> getAllUsersProgressSummary() {
        LOG.trace("Getting progress summary for all users");

        // Fetch all users
        final List<UserEntity> users = UserEntity.listAll();
        if (users.isEmpty()) {
            return List.of();
        }

        // Fetch all sessions in a single query
        final List<StudentSessionEntity> allSessions = StudentSessionEntity.listAll();

        // Group sessions by user ID (filter out sessions with null user to avoid NPE)
        final Map<Long, List<StudentSessionEntity>> sessionsByUser = allSessions.stream()
                .filter(session -> session.user != null)
                .collect(Collectors.groupingBy(session -> session.user.id));

        // Build progress summaries for each user
        return users.stream()
                .map(user -> this.computeProgressSummary(user,
                        sessionsByUser.getOrDefault(user.id, List.of())))
                .filter(summary -> summary != null)
                .toList();
    }

    /**
     * Computes progress summary for a user given their sessions
     * Helper method to avoid code duplication between getUserProgressSummary and
     * getAllUsersProgressSummary
     */
    private StudentProgressSummaryDto computeProgressSummary(final UserEntity user,
            final List<StudentSessionEntity> sessions) {
        if (sessions.isEmpty()) {
            return new StudentProgressSummaryDto(
                    user.id,
                    user.username,
                    0, 0, 0, 0, 0, 0.0, 0.0, null);
        }

        final int totalSessions = sessions.size();
        final int completedSessions = (int) sessions.stream()
                .filter(s -> s.completed != null && s.completed)
                .count();

        // Note: one problem = one session/exercise attempt
        // totalProblems is the number of exercises attempted
        final int totalProblems = totalSessions;

        // completedProblems is the number of exercises successfully completed
        final int completedProblems = completedSessions;

        final int hintsUsed = sessions.stream()
                .mapToInt(s -> s.hintsUsed != null ? s.hintsUsed : 0)
                .sum();

        // Average actions per problem (per session)
        final double totalActions = sessions.stream()
                .mapToInt(s -> s.actionsCount != null ? s.actionsCount : 0)
                .sum();
        final double averageActionsPerProblem = totalSessions > 0 ? totalActions / totalSessions : 0.0;

        // Success rate: percentage of exercises that were completed successfully
        final double successRate = totalSessions > 0 ? (double) completedSessions / totalSessions : 0.0;

        final LocalDateTime lastActivity = sessions.stream()
                .map(s -> s.endTime != null ? s.endTime : s.startTime)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return new StudentProgressSummaryDto(
                user.id,
                user.username,
                totalSessions,
                completedSessions,
                totalProblems,
                completedProblems,
                hintsUsed,
                averageActionsPerProblem,
                successRate,
                lastActivity);
    }

    /**
     * Get statistics by problem category
     * Returns a map of category name to number of problems solved
     */
    @Transactional
    public Map<String, Integer> getProblemCategoryStats() {
        LOG.trace("Getting problem category statistics");

        final List<StudentSessionEntity> completedSessions = StudentSessionEntity
                .find("completed = true")
                .list();

        return completedSessions.stream()
                .collect(Collectors.groupingBy(
                        session -> session.exercise != null ? session.exercise.title
                                : "Unknown",
                        Collectors.summingInt(session -> 1)));
    }

    /**
     * Get total sessions count
     */
    @Transactional
    public long getTotalSessionsCount() {
        LOG.trace("Getting total sessions count");
        return StudentSessionEntity.count();
    }

    /**
     * Get completed sessions count
     */
    @Transactional
    public long getCompletedSessionsCount() {
        LOG.trace("Getting completed sessions count");
        return StudentSessionEntity.find("completed = true").count();
    }

    /**
     * Get active students count (students with sessions in last 7 days)
     */
    @Transactional
    public long getActiveStudentsCount() {
        LOG.trace("Getting active students count");
        final LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        final List<StudentSessionEntity> recentSessions = StudentSessionEntity
                .find("startTime >= ?1", sevenDaysAgo)
                .list();

        return recentSessions.stream()
                .map(s -> s.user.id)
                .distinct()
                .count();
    }

    /**
     * Get sessions count for today
     */
    @Transactional
    public long getTodaySessionsCount() {
        LOG.trace("Getting today's sessions count");
        final LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        final LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);

        return StudentSessionEntity
                .find("startTime >= ?1 AND startTime <= ?2", startOfDay, endOfDay)
                .count();
    }

    /**
     * Search sessions by student username or exercise title
     */
    @Transactional
    public List<StudentSessionViewDto> searchSessions(final String searchTerm) {
        LOG.trace("Searching sessions for term: {}", searchTerm);
        final String pattern = "%" + searchTerm.toLowerCase() + "%";
        final List<StudentSessionEntity> sessions = StudentSessionEntity
                .find("LOWER(user.username) LIKE ?1 OR LOWER(exercise.title) LIKE ?1", pattern, pattern)
                .list();
        return sessions.stream()
                .map(StudentSessionViewDto::new)
                .toList();
    }
}
