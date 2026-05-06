package de.vptr.aimathtutor.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import de.vptr.aimathtutor.dto.AiInteractionViewDto;
import de.vptr.aimathtutor.dto.StudentProgressSummaryDto;
import de.vptr.aimathtutor.dto.StudentSessionViewDto;
import de.vptr.aimathtutor.entity.AiInteractionEntity;
import de.vptr.aimathtutor.entity.StudentSessionEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.repository.AiInteractionRepository;
import de.vptr.aimathtutor.repository.StudentSessionRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Service for retrieving analytics and progress tracking data.
 * Used by admin views to display student sessions, AI interactions, and
 * progress summaries.
 */
@ApplicationScoped
public class AnalyticsService {

    private static final Logger LOG = Logger.getLogger(AnalyticsService.class);

    /**
     * Repository injections
     */
    @Inject
    StudentSessionRepository studentSessionRepository;

    @Inject
    AiInteractionRepository aiInteractionRepository;

    @Inject
    UserRepository userRepository;

    /**
     * Get all student sessions
     */
    @Transactional
    public List<StudentSessionViewDto> getAllSessions() {
        LOG.trace("Getting all student sessions");
        final List<StudentSessionEntity> sessions = this.studentSessionRepository.findAll();
        return sessions.stream()
                .map(StudentSessionViewDto::new)
                .toList();
    }

    /**
     * Get sessions by user ID
     */
    @Transactional
    public List<StudentSessionViewDto> getSessionsByUser(final Long userId) {
        LOG.tracef("Getting sessions for user: %s", userId);
        final List<StudentSessionEntity> sessions = this.studentSessionRepository.findByUserId(userId);
        return sessions.stream()
                .map(StudentSessionViewDto::new)
                .toList();
    }

    /**
     * Get sessions by exercise ID
     */
    @Transactional
    public List<StudentSessionViewDto> getSessionsByExercise(final Long exerciseId) {
        LOG.tracef("Getting sessions for exercise: %s", exerciseId);
        final List<StudentSessionEntity> sessions = this.studentSessionRepository.findByExerciseId(exerciseId);
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
        LOG.tracef("Getting sessions for user: %s on exercise: %s", userId, exerciseId);
        // Use repository single-query filter
        final List<StudentSessionEntity> sessions = this.studentSessionRepository
                .findByUserIdAndExerciseId(userId, exerciseId);
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
        LOG.tracef("Getting sessions for user: %s between %s and %s", userId, startDate, endDate);
        // repository can expose a date-range finder if needed; use filter here
        final List<StudentSessionEntity> sessions = this.studentSessionRepository
                .findByUserIdAndDateRange(userId, startDate, endDate);
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
        LOG.tracef("Getting sessions for exercise: %s between %s and %s", exerciseId, startDate, endDate);
        final List<StudentSessionEntity> sessions = this.studentSessionRepository
                .findByExerciseIdAndDateRange(exerciseId, startDate, endDate);
        return sessions.stream()
                .map(StudentSessionViewDto::new)
                .toList();
    }

    /**
     * Get sessions within a date range. Either bound may be null for an
     * open-ended range.
     */
    @Transactional
    public List<StudentSessionViewDto> getSessionsByDateRange(
            final LocalDateTime startDate,
            final LocalDateTime endDate) {
        LOG.tracef("Getting sessions between %s and %s", startDate, endDate);
        final List<StudentSessionEntity> sessions;
        if (startDate != null && endDate != null) {
            sessions = this.studentSessionRepository.findByStartTimeBetween(startDate, endDate);
        } else if (startDate != null) {
            sessions = this.studentSessionRepository.findByStartTimeAfter(startDate);
        } else if (endDate != null) {
            sessions = this.studentSessionRepository.findByStartTimeBefore(endDate);
        } else {
            sessions = this.studentSessionRepository.findAll();
        }
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
        LOG.tracef("Getting %s sessions between %s and %s",
                Boolean.TRUE.equals(completed) ? "completed" : "incomplete", startDate, endDate);
        final List<StudentSessionEntity> sessions = this.studentSessionRepository
                .findByCompletedAndDateRange(completed, startDate, endDate);
        return sessions.stream()
                .map(StudentSessionViewDto::new)
                .toList();
    }

    /**
     * Get a specific session by ID
     */
    @Transactional
    public StudentSessionViewDto getSessionById(final Long sessionId) {
        LOG.tracef("Getting session: %s", sessionId);
        final StudentSessionEntity session = this.studentSessionRepository.findById(sessionId);
        return session != null ? new StudentSessionViewDto(session) : null;
    }

    /**
     * Get a specific session by session ID string
     */
    @Transactional
    public StudentSessionViewDto getSessionBySessionId(final String sessionId) {
        LOG.tracef("Getting session by session ID: %s", sessionId);
        final StudentSessionEntity session = this.studentSessionRepository
                .findBySessionIdWithRelations(sessionId);
        return session != null ? new StudentSessionViewDto(session) : null;
    }

    /**
     * Get all AI interactions
     */
    @Transactional
    public List<AiInteractionViewDto> getAllAiInteractions() {
        LOG.trace("Getting all AI interactions");
        final List<AiInteractionEntity> interactions = this.aiInteractionRepository.findAll();
        return interactions.stream()
                .map(AiInteractionViewDto::new)
                .toList();
    }

    /**
     * Get AI interactions by session ID
     */
    @Transactional
    public List<AiInteractionViewDto> getAiInteractionsBySession(final String sessionId) {
        LOG.tracef("Getting AI interactions for session: %s", sessionId);
        final List<AiInteractionEntity> interactions = this.aiInteractionRepository.findBySessionId(sessionId);
        return interactions.stream()
                .map(AiInteractionViewDto::new)
                .toList();
    }

    /**
     * Get AI interactions by user ID
     */
    @Transactional
    public List<AiInteractionViewDto> getAiInteractionsByUser(final Long userId) {
        LOG.tracef("Getting AI interactions for user: %s", userId);
        final List<AiInteractionEntity> interactions = this.aiInteractionRepository.findByUserId(userId);
        return interactions.stream()
                .map(AiInteractionViewDto::new)
                .toList();
    }

    /**
     * Get AI interactions by exercise ID
     */
    @Transactional
    public List<AiInteractionViewDto> getAiInteractionsByExercise(final Long exerciseId) {
        LOG.tracef("Getting AI interactions for exercise: %s", exerciseId);
        final List<AiInteractionEntity> interactions = this.aiInteractionRepository
                .findByExerciseId(exerciseId);
        return interactions.stream()
                .map(AiInteractionViewDto::new)
                .toList();
    }

    /**
     * Get progress summary for a single user
     */
    @Transactional
    public StudentProgressSummaryDto getUserProgressSummary(final Long userId) {
        LOG.tracef("Getting progress summary for user: %s", userId);

        final UserEntity user = this.userRepository.findById(userId);
        if (user == null) {
            return null;
        }

        final List<StudentSessionEntity> sessions = this.studentSessionRepository.findByUserId(userId);
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
        final List<UserEntity> users = this.userRepository.findAll();
        if (users.isEmpty()) {
            return List.of();
        }

        // Fetch all sessions in a single query
        final List<StudentSessionEntity> allSessions = this.studentSessionRepository.findAll();

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
                    user.publicId,
                    user.username,
                    0, 0, 0, 0, 0, 0.0, 0.0, null);
        }

        final int totalSessions = sessions.size();
        final int completedSessions = (int) sessions.stream()
                .filter(s -> s.completed)
                .count();

        // Note: one problem = one session/exercise attempt
        // totalProblems is the number of exercises attempted
        final int totalProblems = totalSessions;

        // completedProblems is the number of exercises successfully completed
        final int completedProblems = completedSessions;

        final int hintsUsed = sessions.stream()
                .mapToInt(s -> s.hintsUsed)
                .sum();

        // Average actions per problem (per session)
        final double totalActions = sessions.stream()
                .mapToInt(s -> s.actionsCount)
                .sum();
        final double averageActionsPerProblem = totalSessions > 0 ? totalActions / totalSessions : 0.0;

        /*
         * Success rate: percentage of actions that were correct (problem-solving
         * accuracy).
         * Not the same as completion rate.
         */
        final int totalCorrectActions = sessions.stream()
                .mapToInt(s -> s.correctActions)
                .sum();
        final double successRate = totalActions > 0 ? (double) totalCorrectActions / totalActions : 0.0;

        final LocalDateTime lastActivity = sessions.stream()
                .map(s -> s.endTime != null ? s.endTime : s.startTime)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return new StudentProgressSummaryDto(
                user.publicId,
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

        final List<Object[]> results = this.studentSessionRepository.findProblemCategoryStats();
        return results.stream()
                .collect(Collectors.toMap(
                        row -> row[0] != null ? (String) row[0] : "Unknown",
                        row -> ((Number) row[1]).intValue(),
                        (a, ignored) -> a));
    }

    /**
     * Get total sessions count
     */
    @Transactional
    public long getTotalSessionsCount() {
        LOG.trace("Getting total sessions count");
        // repository can expose count; fall back to list size
        return this.studentSessionRepository.countAll();
    }

    /**
     * Get completed sessions count
     */
    @Transactional
    public long getCompletedSessionsCount() {
        LOG.trace("Getting completed sessions count");
        return this.studentSessionRepository.countByCompleted(Boolean.TRUE);
    }

    /**
     * Get active students count (students with sessions in last 7 days)
     */
    @Transactional
    public long getActiveStudentsCount() {
        LOG.trace("Getting active students count");
        final LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        return this.studentSessionRepository.countActiveStudentsSince(sevenDaysAgo);
    }

    /**
     * Get sessions count for today
     */
    @Transactional
    public long getTodaySessionsCount() {
        LOG.trace("Getting today's sessions count");
        final LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        final LocalDateTime endOfDay = LocalDate.now().plusDays(1).atStartOfDay();
        return this.studentSessionRepository
                .countByStartTimeGreaterThanEqualAndStartTimeLessThan(startOfDay, endOfDay);
    }

    /**
     * Search sessions by student username or exercise title
     */
    @Transactional
    public List<StudentSessionViewDto> searchSessions(final String searchTerm) {
        LOG.tracef("Searching sessions for term: %s", searchTerm);
        if (searchTerm == null || searchTerm.isBlank()) {
            return List.of();
        }
        final String pattern = "%" + searchTerm.trim().toLowerCase() + "%";
        final List<StudentSessionEntity> sessions = this.studentSessionRepository
                .searchByUserOrExerciseTerm(pattern);
        return sessions.stream()
                .map(StudentSessionViewDto::new)
                .toList();
    }

    /**
     * Get all sessions for a user grouped by exercise ID.
     * Used for batch-loading completion data to avoid N+1 queries.
     */
    @Transactional
    public Map<String, List<StudentSessionViewDto>> getSessionsByUserGroupedByExercise(final Long userId) {
        LOG.tracef("Getting sessions grouped by exercise for user: %s", userId);
        if (userId == null) {
            return Map.of();
        }
        final List<StudentSessionEntity> sessions = this.studentSessionRepository.findByUserId(userId);
        return sessions.stream()
                .map(StudentSessionViewDto::new)
                .collect(Collectors.groupingBy(
                        s -> s.exercisePublicId != null ? s.exercisePublicId : ""));
    }

    /**
     * Get progress summaries for users whose last activity falls within a date
     * range. Full session history is used for each user so totals remain
     * consistent with other summaries.
     */
    @Transactional
    public List<StudentProgressSummaryDto> getUsersProgressSummaryByDateRange(
            final LocalDateTime startDate,
            final LocalDateTime endDate) {
        LOG.tracef("Getting progress summaries between %s and %s", startDate, endDate);

        final List<StudentSessionEntity> rangeSessions = this.studentSessionRepository
                .findByStartTimeBetween(startDate, endDate);
        if (rangeSessions.isEmpty()) {
            return List.of();
        }

        final Map<Long, List<StudentSessionEntity>> sessionsByUser = rangeSessions.stream()
                .filter(session -> session.user != null)
                .collect(Collectors.groupingBy(session -> session.user.id));

        final List<Long> userIds = sessionsByUser.keySet().stream().toList();
        final List<StudentSessionEntity> allUserSessions = this.studentSessionRepository
                .findByUserIdIn(userIds);
        final Map<Long, List<StudentSessionEntity>> allSessionsByUser = allUserSessions.stream()
                .filter(session -> session.user != null)
                .collect(Collectors.groupingBy(session -> session.user.id));

        return sessionsByUser.entrySet().stream()
                .map(entry -> {
                    final UserEntity user = entry.getValue().stream()
                            .map(s -> s.user)
                            .filter(u -> u != null)
                            .findFirst()
                            .orElse(null);
                    if (user == null) {
                        return null;
                    }
                    return this.computeProgressSummary(user,
                            allSessionsByUser.getOrDefault(entry.getKey(), List.of()));
                })
                .filter(summary -> summary != null)
                .toList();
    }

    /**
     * Get progress summaries for users matching a username search term.
     */
    @Transactional
    public List<StudentProgressSummaryDto> getUsersProgressSummaryByUsernameSearch(final String searchTerm) {
        LOG.tracef("Getting progress summaries for username search: %s", searchTerm);
        if (searchTerm == null || searchTerm.isBlank()) {
            return this.getAllUsersProgressSummary();
        }

        final String pattern = "%" + searchTerm.trim().toLowerCase() + "%";
        final List<UserEntity> users = this.userRepository.search(pattern);
        if (users.isEmpty()) {
            return List.of();
        }

        final List<Long> userIds = users.stream().map(u -> u.id).toList();
        final List<StudentSessionEntity> userSessions = this.studentSessionRepository.findByUserIdIn(userIds);
        final Map<Long, List<StudentSessionEntity>> sessionsByUser = userSessions.stream()
                .filter(session -> session.user != null)
                .collect(Collectors.groupingBy(session -> session.user.id));

        return users.stream()
                .map(user -> this.computeProgressSummary(user,
                        sessionsByUser.getOrDefault(user.id, List.of())))
                .filter(summary -> summary != null)
                .toList();
    }
}
