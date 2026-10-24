package de.vptr.aimathtutor.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import de.vptr.aimathtutor.dto.AiInteractionViewDto;
import de.vptr.aimathtutor.dto.DashboardTrendDto;
import de.vptr.aimathtutor.dto.SessionCredentials;
import de.vptr.aimathtutor.dto.StudentProgressSummaryDto;
import de.vptr.aimathtutor.dto.StudentSessionViewDto;
import de.vptr.aimathtutor.entity.AiInteractionEntity;
import de.vptr.aimathtutor.entity.StudentSessionEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.exception.PermissionDeniedException;
import de.vptr.aimathtutor.repository.AiInteractionRepository;
import de.vptr.aimathtutor.repository.ExerciseRepository;
import de.vptr.aimathtutor.repository.StudentSessionRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.security.PermissionService;
import de.vptr.aimathtutor.util.SearchPatternUtil;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;

/**
 * Service for retrieving analytics and progress tracking data. Used by admin views to display student sessions, AI
 * interactions, and progress summaries.
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

    @Inject
    ExerciseRepository exerciseRepository;

    @Inject
    EntityManager entityManager;

    @Inject
    PermissionService permissionService;

    /**
     * Get all student sessions
     *
     * @param credentials
     *            the credentials of the calling session, captured on the UI thread
     * @throws ValidationException
     *             if the session has ended
     * @throws PermissionDeniedException
     *             if the caller may not view the administration
     */
    @Transactional
    public List<StudentSessionViewDto> getAllSessions(@Nullable final SessionCredentials credentials) {
        this.permissionService.requireAdminView(credentials);
        LOG.trace("Getting all student sessions");
        final List<StudentSessionEntity> sessions = this.studentSessionRepository.findAll();
        return sessions.stream().map(StudentSessionViewDto::new).toList();
    }

    /**
     * Get sessions by user and exercise ID Efficient single-database query for filtering sessions by both user and
     * exercise
     */
    @Transactional
    public List<StudentSessionViewDto> getSessionsByUserAndExercise(final Long userId,
            @Nullable final Long exerciseId) {
        LOG.tracef("Getting sessions for user: %s on exercise: %s", userId, exerciseId);
        // Use repository single-query filter
        final List<StudentSessionEntity> sessions =
                this.studentSessionRepository.findByUserIdAndExerciseId(userId, exerciseId);
        return sessions.stream().map(StudentSessionViewDto::new).toList();
    }

    /**
     * Get sessions within a date range. Either bound may be null for an open-ended range.
     *
     * @param credentials
     *            the credentials of the calling session, captured on the UI thread
     * @param startDate
     *            the inclusive lower bound, or null
     * @param endDate
     *            the inclusive upper bound, or null
     * @throws ValidationException
     *             if the session has ended
     * @throws PermissionDeniedException
     *             if the caller may not view the administration
     */
    @Transactional
    public List<StudentSessionViewDto> getSessionsByDateRange(@Nullable final SessionCredentials credentials,
            final LocalDateTime startDate, final LocalDateTime endDate) {
        this.permissionService.requireAdminView(credentials);
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
        return sessions.stream().map(StudentSessionViewDto::new).toList();
    }

    /**
     * Get a specific session by session ID string
     *
     * @param credentials
     *            the credentials of the calling session, captured on the UI thread
     * @param sessionId
     *            the session ID string
     * @throws ValidationException
     *             if the session has ended
     * @throws PermissionDeniedException
     *             if the caller may not view the administration
     */
    @Transactional
    @Nullable
    public StudentSessionViewDto getSessionBySessionId(@Nullable final SessionCredentials credentials,
            final String sessionId) {
        this.permissionService.requireAdminView(credentials);
        LOG.tracef("Getting session by session ID: %s", sessionId);
        final StudentSessionEntity session = this.studentSessionRepository.findBySessionIdWithRelations(sessionId);
        return session != null ? new StudentSessionViewDto(session) : null;
    }

    /**
     * Get AI interactions by session ID
     *
     * @param credentials
     *            the credentials of the calling session, captured on the UI thread
     * @param sessionId
     *            the session ID string
     * @throws ValidationException
     *             if the session has ended
     * @throws PermissionDeniedException
     *             if the caller may not view the administration
     */
    @Transactional
    public List<AiInteractionViewDto> getAiInteractionsBySession(@Nullable final SessionCredentials credentials,
            final String sessionId) {
        this.permissionService.requireAdminView(credentials);
        LOG.tracef("Getting AI interactions for session: %s", sessionId);
        final List<AiInteractionEntity> interactions = this.aiInteractionRepository.findBySessionId(sessionId);
        return interactions.stream().map(AiInteractionViewDto::new).toList();
    }

    /**
     * Get progress summaries for all users Refactored to avoid N+1 queries by fetching all sessions once and grouping
     * by user
     *
     * @param credentials
     *            the credentials of the calling session, captured on the UI thread
     * @throws ValidationException
     *             if the session has ended
     * @throws PermissionDeniedException
     *             if the caller may not view the administration
     */
    @Transactional
    public List<StudentProgressSummaryDto> getAllUsersProgressSummary(@Nullable final SessionCredentials credentials) {
        this.permissionService.requireAdminView(credentials);
        return this.findAllUsersProgressSummary();
    }

    private List<StudentProgressSummaryDto> findAllUsersProgressSummary() {
        LOG.trace("Getting progress summary for all users");

        // Fetch all users
        final List<UserEntity> users = this.userRepository.findAll();
        if (users.isEmpty()) {
            return List.of();
        }

        // Fetch all sessions in a single query
        final List<StudentSessionEntity> allSessions = this.studentSessionRepository.findAll();

        // Build progress summaries for each user
        return this.buildProgressSummaries(users, allSessions);
    }

    private List<StudentProgressSummaryDto> buildProgressSummaries(final List<UserEntity> users,
            final List<StudentSessionEntity> sessions) {
        final Map<Long, List<StudentSessionEntity>> sessionsByUser =
                sessions.stream().filter(session -> session.user != null)
                        .collect(Collectors.groupingBy(session -> Objects.requireNonNull(session.user).id));
        return users.stream()
                .map(user -> this.computeProgressSummary(user, sessionsByUser.getOrDefault(user.id, List.of())))
                .filter(summary -> summary != null).toList();
    }

    /**
     * Computes progress summary for a user given their sessions
     */
    private StudentProgressSummaryDto computeProgressSummary(final UserEntity user,
            final List<StudentSessionEntity> sessions) {
        final int completedSessions = (int) sessions.stream().filter(s -> s.completed).count();
        final int hintsUsed = sessions.stream().mapToInt(s -> s.hintsUsed).sum();
        final long totalActions = sessions.stream().mapToInt(s -> s.actionsCount).sum();
        final long totalCorrectActions = sessions.stream().mapToInt(s -> s.correctActions).sum();
        final LocalDateTime lastActivity = sessions.stream().map(s -> s.endTime != null ? s.endTime : s.startTime)
                .max(LocalDateTime::compareTo).orElse(null);
        return buildProgressSummary(user, sessions.size(), completedSessions, hintsUsed, totalActions,
                totalCorrectActions, lastActivity);
    }

    /**
     * Builds a summary from a user's session totals. One problem = one session/exercise attempt, so the problem counts
     * equal the session counts. The success rate is the share of actions that were correct (problem-solving accuracy),
     * not the completion rate.
     */
    private static StudentProgressSummaryDto buildProgressSummary(final UserEntity user, final int totalSessions,
            final int completedSessions, final int hintsUsed, final long totalActions, final long totalCorrectActions,
            @Nullable final LocalDateTime lastActivity) {
        final double averageActionsPerProblem = totalSessions > 0 ? (double) totalActions / totalSessions : 0.0;
        final double successRate = totalActions > 0 ? (double) totalCorrectActions / totalActions : 0.0;
        return new StudentProgressSummaryDto(user.publicId, user.username, totalSessions, completedSessions,
                totalSessions, completedSessions, hintsUsed, averageActionsPerProblem, successRate, lastActivity);
    }

    /**
     * Get the number of completed sessions per exercise title. Returns a map of exercise title to that number.
     */
    @Transactional
    public Map<String, Integer> getCompletedSessionsByExercise() {
        LOG.trace("Getting completed sessions by exercise");

        final List<Object[]> results = this.studentSessionRepository.findCompletedSessionsByExercise();
        return results.stream().collect(Collectors.toMap(row -> row[0] != null ? (String) row[0] : "Unknown",
                row -> ((Number) row[1]).intValue(), (a, ignored) -> a));
    }

    /**
     * Get total sessions count
     */
    @Transactional
    public long getTotalSessionsCount() {
        LOG.trace("Getting total sessions count");
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
        final LocalDateTime sevenDaysAgo = LocalDateTime.now(ZoneId.systemDefault()).minusDays(7);
        return this.studentSessionRepository.countActiveStudentsSince(sevenDaysAgo);
    }

    /**
     * Get sessions count for today
     */
    @Transactional
    public long getTodaySessionsCount() {
        LOG.trace("Getting today's sessions count");
        final LocalDateTime startOfDay = LocalDate.now(ZoneId.systemDefault()).atStartOfDay();
        final LocalDateTime endOfDay = LocalDate.now(ZoneId.systemDefault()).plusDays(1).atStartOfDay();
        return this.studentSessionRepository.countByStartTimeGreaterThanEqualAndStartTimeLessThan(startOfDay, endOfDay);
    }

    /**
     * Search sessions by student username or exercise title
     *
     * @param credentials
     *            the credentials of the calling session, captured on the UI thread
     * @param searchTerm
     *            the search term
     * @throws ValidationException
     *             if the session has ended
     * @throws PermissionDeniedException
     *             if the caller may not view the administration
     */
    @Transactional
    public List<StudentSessionViewDto> searchSessions(@Nullable final SessionCredentials credentials,
            final String searchTerm) {
        this.permissionService.requireAdminView(credentials);
        LOG.tracef("Searching sessions for term: %s", searchTerm);
        if (searchTerm == null || searchTerm.isBlank()) {
            return List.of();
        }
        final String pattern = SearchPatternUtil.containsPattern(searchTerm.trim().toLowerCase(Locale.ROOT));
        final List<StudentSessionEntity> sessions = this.studentSessionRepository.searchByUserOrExerciseTerm(pattern);
        return sessions.stream().map(StudentSessionViewDto::new).toList();
    }

    /**
     * Get all sessions for a user grouped by exercise ID. Used for batch-loading completion data to avoid N+1 queries.
     */
    @Transactional
    public Map<String, List<StudentSessionViewDto>> getSessionsByUserGroupedByExercise(final Long userId) {
        LOG.tracef("Getting sessions grouped by exercise for user: %s", userId);
        if (userId == null) {
            return Map.of();
        }
        final List<StudentSessionEntity> sessions = this.studentSessionRepository.findByUserId(userId);
        return sessions.stream().map(StudentSessionViewDto::new)
                .collect(Collectors.groupingBy(s -> s.exercisePublicId != null ? s.exercisePublicId : ""));
    }

    /**
     * Get progress summaries for users whose last activity falls within a date range. Full session history is used for
     * each user so totals remain consistent with other summaries.
     *
     * @param credentials
     *            the credentials of the calling session, captured on the UI thread
     * @param startDate
     *            the start of the range
     * @param endDate
     *            the end of the range
     * @throws ValidationException
     *             if the session has ended
     * @throws PermissionDeniedException
     *             if the caller may not view the administration
     */
    @Transactional
    public List<StudentProgressSummaryDto> getUsersProgressSummaryByDateRange(
            @Nullable final SessionCredentials credentials, final LocalDateTime startDate,
            final LocalDateTime endDate) {
        this.permissionService.requireAdminView(credentials);
        LOG.tracef("Getting progress summaries between %s and %s", startDate, endDate);

        final List<StudentSessionEntity> rangeSessions =
                this.studentSessionRepository.findByStartTimeBetween(startDate, endDate);
        if (rangeSessions.isEmpty()) {
            return List.of();
        }

        final Map<Long, List<StudentSessionEntity>> sessionsByUser =
                rangeSessions.stream().filter(session -> session.user != null)
                        .collect(Collectors.groupingBy(session -> Objects.requireNonNull(session.user).id));

        final List<Long> userIds = sessionsByUser.keySet().stream().toList();
        final List<StudentSessionEntity> allUserSessions = this.studentSessionRepository.findByUserIdIn(userIds);
        final Map<Long, List<StudentSessionEntity>> allSessionsByUser =
                allUserSessions.stream().filter(session -> session.user != null)
                        .collect(Collectors.groupingBy(session -> Objects.requireNonNull(session.user).id));

        return sessionsByUser.entrySet().stream().map(entry -> {
            final UserEntity user =
                    entry.getValue().stream().map(s -> s.user).filter(u -> u != null).findFirst().orElse(null);
            if (user == null) {
                return null;
            }
            return this.computeProgressSummary(user, allSessionsByUser.getOrDefault(entry.getKey(), List.of()));
        }).filter(summary -> summary != null).toList();
    }

    /**
     * Get progress summaries for users matching a username search term.
     *
     * @param credentials
     *            the credentials of the calling session, captured on the UI thread
     * @param searchTerm
     *            the username search term
     * @throws ValidationException
     *             if the session has ended
     * @throws PermissionDeniedException
     *             if the caller may not view the administration
     */
    @Transactional
    public List<StudentProgressSummaryDto> getUsersProgressSummaryByUsernameSearch(
            @Nullable final SessionCredentials credentials, final String searchTerm) {
        this.permissionService.requireAdminView(credentials);
        LOG.tracef("Getting progress summaries for username search with query length: %s",
                searchTerm == null ? 0 : searchTerm.length());
        if (searchTerm == null || searchTerm.isBlank()) {
            return this.findAllUsersProgressSummary();
        }

        // The repository takes the RAW term and builds the LIKE pattern (or email lookup) itself.
        final List<UserEntity> users = this.userRepository.search(searchTerm.trim());
        if (users.isEmpty()) {
            return List.of();
        }

        final List<Long> userIds = users.stream().map(u -> u.id).toList();
        final List<StudentSessionEntity> userSessions = this.studentSessionRepository.findByUserIdIn(userIds);
        return this.buildProgressSummaries(users, userSessions);
    }

    /**
     * Get total user count.
     */
    @Transactional
    public long getUserCount() {
        LOG.trace("Getting user count");
        return this.userRepository.countAll();
    }

    /**
     * Get published exercise count.
     */
    @Transactional
    public long getPublishedExerciseCount() {
        LOG.trace("Getting published exercise count");
        return this.exerciseRepository.countPublished();
    }

    /**
     * Get daily session counts for today and the N days before it. Returns a LinkedHashMap with every day filled (0 for
     * missing days); counting runs in SQL (GROUP BY start date).
     */
    @Transactional
    public Map<LocalDate, Long> getDailySessionCounts(final int days) {
        LOG.tracef("Getting daily session counts for last %d days", days);
        final var zone = ZoneId.systemDefault();
        final var now = LocalDateTime.now(zone);
        final var today = now.toLocalDate();
        final var counts = new LinkedHashMap<LocalDate, Long>();
        for (int i = days; i >= 0; i--) {
            counts.put(today.minusDays(i), 0L);
        }
        for (final Object[] row : this.studentSessionRepository
                .countSessionsPerDay(today.minusDays(days).atStartOfDay(), now)) {
            counts.merge((LocalDate) row[0], ((Number) row[1]).longValue(), Long::sum);
        }
        return counts;
    }

    /**
     * Get trend data comparing current 7-day period to the prior 7-day period.
     */
    @Transactional
    public DashboardTrendDto getTrendData() {
        LOG.trace("Getting dashboard trend data");
        final var now = LocalDateTime.now(ZoneId.systemDefault());
        final var weekAgo = now.minusDays(7);
        final var twoWeeksAgo = now.minusDays(14);

        final var totalSessions = this.studentSessionRepository.countByStartTimeBetween(weekAgo, now);
        final var prevTotalSessions = this.studentSessionRepository.countByStartTimeBetween(twoWeeksAgo, weekAgo);

        final var completedSessions =
                this.studentSessionRepository.countByCompletedAndStartTimeBetween(Boolean.TRUE, weekAgo, now);
        final var prevCompletedSessions =
                this.studentSessionRepository.countByCompletedAndStartTimeBetween(Boolean.TRUE, twoWeeksAgo, weekAgo);

        final var activeStudents = this.studentSessionRepository.countActiveStudentsSince(weekAgo);
        final var prevActiveStudents = this.studentSessionRepository.countActiveStudentsBetween(twoWeeksAgo, weekAgo);

        final var startOfToday = LocalDate.now(ZoneId.systemDefault()).atStartOfDay();
        final var startOfTomorrow = startOfToday.plusDays(1);
        final var todaySessions = this.studentSessionRepository
                .countByStartTimeGreaterThanEqualAndStartTimeLessThan(startOfToday, startOfTomorrow);
        final var prevTodaySessions =
                this.studentSessionRepository.countByStartTimeGreaterThanEqualAndStartTimeLessThan(
                        startOfToday.minusDays(7), startOfTomorrow.minusDays(7));

        final var totalUsers =
                ((Number) this.entityManager.createQuery("SELECT COUNT(u) FROM UserEntity u").getSingleResult())
                        .longValue();
        final var prevTotalUsers =
                ((Number) this.entityManager.createQuery("SELECT COUNT(u) FROM UserEntity u WHERE u.created < :d")
                        .setParameter("d", weekAgo).getSingleResult()).longValue();

        final var publishedExercises = ((Number) this.entityManager
                .createQuery("SELECT COUNT(e) FROM ExerciseEntity e WHERE e.published = true").getSingleResult())
                .longValue();
        final var prevPublishedExercises = ((Number) this.entityManager
                .createQuery("SELECT COUNT(e) FROM ExerciseEntity e WHERE e.published = true AND e.created < :d")
                .setParameter("d", weekAgo).getSingleResult()).longValue();

        return new DashboardTrendDto(totalSessions, prevTotalSessions, completedSessions, prevCompletedSessions,
                activeStudents, prevActiveStudents, todaySessions, prevTodaySessions, totalUsers, prevTotalUsers,
                publishedExercises, prevPublishedExercises);
    }

    /**
     * Get completion rate histogram across individual sessions, bucketed by session accuracy (correct_actions /
     * actions_count): 0%, 1-25%, 26-50%, 51-75%, 76-99%, 100%. Aggregation runs in SQL via GROUP BY so the full
     * sessions table is never materialised in memory.
     */
    @Transactional
    public Map<String, Integer> getCompletionRateHistogram() {
        LOG.trace("Getting completion rate histogram");
        final var buckets = new LinkedHashMap<String, Integer>();
        buckets.put("0%", 0);
        buckets.put("1-25%", 0);
        buckets.put("26-50%", 0);
        buckets.put("51-75%", 0);
        buckets.put("76-99%", 0);
        buckets.put("100%", 0);
        for (final Object[] row : this.studentSessionRepository.findCompletionRateBuckets()) {
            final String label = (String) row[0];
            final int count = ((Number) row[1]).intValue();
            if (label != null && buckets.containsKey(label)) {
                buckets.put(label, count);
            }
        }
        return buckets;
    }

    /**
     * Get the mean, over users, of each user's own success rate (correct actions / actions across all their sessions),
     * counting only users with at least one action. Aggregation runs in SQL.
     *
     * @return the mean rate in [0, 1], or 0.0 when no user qualifies
     */
    @Transactional
    public double getAverageSuccessRate() {
        LOG.trace("Getting average success rate");
        return this.studentSessionRepository.findAverageUserSuccessRate();
    }

    /**
     * Get hint usage distribution across individual sessions: 0 hints, 1-3, 4-7, 8+. Aggregation runs in SQL.
     */
    @Transactional
    public Map<String, Integer> getHintUsageBuckets() {
        LOG.trace("Getting hint usage buckets");
        final var buckets = new LinkedHashMap<String, Integer>();
        buckets.put("0 hints", 0);
        buckets.put("1-3 hints", 0);
        buckets.put("4-7 hints", 0);
        buckets.put("8+ hints", 0);
        for (final Object[] row : this.studentSessionRepository.findHintUsageBuckets()) {
            final String label = (String) row[0];
            final int count = ((Number) row[1]).intValue();
            if (label != null && buckets.containsKey(label)) {
                buckets.put(label, count);
            }
        }
        return buckets;
    }

    /**
     * Get recent sessions within the last 7 days, limited to the given count. Uses {@code LIMIT} at the database so the
     * full 7-day window is never materialised in memory.
     *
     * @param credentials
     *            the credentials of the calling session, captured on the UI thread
     * @param limit
     *            the maximum number of sessions
     * @throws ValidationException
     *             if the session has ended
     * @throws PermissionDeniedException
     *             if the caller may not view the administration
     */
    @Transactional
    public List<StudentSessionViewDto> getRecentSessions(@Nullable final SessionCredentials credentials,
            final int limit) {
        this.permissionService.requireAdminView(credentials);
        LOG.tracef("Getting recent sessions, limit %d", limit);
        final var now = LocalDateTime.now(ZoneId.systemDefault());
        final var weekAgo = now.minusDays(7);
        final var sessions = this.studentSessionRepository.findRecentByStartTimeBetween(weekAgo, now, limit);
        return sessions.stream().map(StudentSessionViewDto::new).toList();
    }

    /**
     * Get top students by completed sessions count, limited to the given count. Resolves the top user IDs via a SQL
     * {@code GROUP BY ... ORDER BY ... LIMIT} query, totals those users' sessions in one more {@code GROUP BY} query
     * (no session is loaded) and looks the users up one by one, so it runs {@code 2 + limit} queries at most.
     *
     * @param credentials
     *            the credentials of the calling session, captured on the UI thread
     * @param limit
     *            the maximum number of students
     * @throws ValidationException
     *             if the session has ended
     * @throws PermissionDeniedException
     *             if the caller may not view the administration
     */
    @Transactional
    public List<StudentProgressSummaryDto> getTopStudentsByCompletion(@Nullable final SessionCredentials credentials,
            final int limit) {
        this.permissionService.requireAdminView(credentials);
        LOG.tracef("Getting top students by completion, limit %d", limit);
        if (limit <= 0) {
            return List.of();
        }
        final List<Object[]> topRows = this.studentSessionRepository.findTopUsersByCompletion(limit);
        if (topRows.isEmpty()) {
            return List.of();
        }
        final List<Long> userIds = topRows.stream().map(row -> ((Number) row[0]).longValue()).toList();
        final Map<Long, Object[]> totalsByUser = this.studentSessionRepository.findProgressTotalsByUserIdIn(userIds)
                .stream().collect(Collectors.toMap(row -> ((Number) row[0]).longValue(), row -> row));

        // Preserve the SQL ordering (highest completion count first). No by-ids user finder exists: at most `limit`
        // primary-key lookups, and a user that no longer exists is skipped.
        final List<StudentProgressSummaryDto> summaries = new ArrayList<>();
        for (final Long uid : userIds) {
            final UserEntity user = this.userRepository.findById(uid);
            final Object[] t = totalsByUser.get(uid);
            if (user != null && t != null) {
                summaries.add(buildProgressSummary(user, ((Number) t[1]).intValue(), ((Number) t[2]).intValue(),
                        ((Number) t[3]).intValue(), ((Number) t[4]).longValue(), ((Number) t[5]).longValue(),
                        (LocalDateTime) t[6]));
            }
        }
        return summaries;
    }
}
