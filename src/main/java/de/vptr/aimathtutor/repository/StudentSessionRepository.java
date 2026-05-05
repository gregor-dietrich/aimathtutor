package de.vptr.aimathtutor.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import de.vptr.aimathtutor.entity.StudentSessionEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Repository for StudentSessionEntity access. Provides common queries used by
 * the admin and analytics services. Null-safe and returns empty lists when
 * parameters are missing.
 */
@ApplicationScoped
public class StudentSessionRepository extends AbstractRepository {

    /**
     * Find a student session by its external session id.
     *
     * @param sessionId external session identifier
     * @return matching StudentSessionEntity or null if none found or sessionId
     *         is null
     */
    public StudentSessionEntity findBySessionId(final String sessionId) {
        if (sessionId == null) {
            return null;
        }
        final var q = this.em.createNamedQuery("StudentSession.findBySessionId", StudentSessionEntity.class);
        q.setParameter("s", sessionId);
        q.setMaxResults(1);
        return q.getResultStream().findFirst().orElse(null);
    }

    /**
     * Retrieves a student session by its public identifier.
     *
     * @param publicId the public ID of the session
     * @return an {@link Optional} containing the session if found, empty otherwise
     */
    public Optional<StudentSessionEntity> findByPublicId(final String publicId) {
        if (publicId == null) {
            return Optional.empty();
        }
        final var q = this.em.createNamedQuery("StudentSession.findByPublicId", StudentSessionEntity.class);
        q.setParameter("p", publicId);
        q.setMaxResults(1);
        return q.getResultStream().findFirst();
    }

    /**
     * Find a student session by its external session id with relations eagerly
     * loaded.
     *
     * @param sessionId external session identifier
     * @return matching StudentSessionEntity or null if none found or sessionId
     *         is null
     */
    public StudentSessionEntity findBySessionIdWithRelations(final String sessionId) {
        if (sessionId == null) {
            return null;
        }
        final var q = this.em.createQuery(
                "SELECT s FROM StudentSessionEntity s LEFT JOIN FETCH s.user LEFT JOIN FETCH s.exercise WHERE s.sessionId = :s",
                StudentSessionEntity.class);
        q.setParameter("s", sessionId);
        q.setMaxResults(1);
        return q.getResultStream().findFirst().orElse(null);
    }

    /**
     * Find all sessions for a given user id with relations eagerly loaded.
     *
     * @param userId database id of the user
     * @return list of sessions, empty list if userId is null or none found
     */
    public List<StudentSessionEntity> findByUserId(final Long userId) {
        if (userId == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("StudentSession.findByUserIdWithRelations", StudentSessionEntity.class);
        q.setParameter("u", userId);
        return q.getResultList();
    }

    /**
     * Find all sessions for a given exercise id with relations eagerly loaded.
     *
     * @param exerciseId database id of the exercise
     * @return list of sessions, empty list if exerciseId is null or none found
     */
    public List<StudentSessionEntity> findByExerciseId(final Long exerciseId) {
        if (exerciseId == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("StudentSession.findByExerciseIdWithRelations",
                StudentSessionEntity.class);
        q.setParameter("e", exerciseId);
        return q.getResultList();
    }

    /**
     * List all student sessions ordered by id descending with relations eagerly
     * loaded.
     *
     * @return list of all student sessions
     */
    public List<StudentSessionEntity> findAll() {
        return this.listNamed("StudentSession.findAllWithRelations", StudentSessionEntity.class);
    }

    /**
     * Find a session by database id.
     *
     * @param id primary key of the session
     * @return session entity or null if id is null or not found
     */
    public StudentSessionEntity findById(final Long id) {
        if (id == null) {
            return null;
        }
        final var q = this.em.createQuery(
                "SELECT s FROM StudentSessionEntity s LEFT JOIN FETCH s.user LEFT JOIN FETCH s.exercise WHERE s.id = :id",
                StudentSessionEntity.class);
        q.setParameter("id", id);
        q.setMaxResults(1);
        return q.getResultStream().findFirst().orElse(null);
    }

    /**
     * Find sessions by user and exercise in a single DB query with relations
     * eagerly loaded.
     */
    public List<StudentSessionEntity> findByUserIdAndExerciseId(final Long userId, final Long exerciseId) {
        if (userId == null || exerciseId == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("StudentSession.findByUserAndExerciseWithRelations",
                StudentSessionEntity.class);
        q.setParameter("u", userId);
        q.setParameter("e", exerciseId);
        return q.getResultList();
    }

    /**
     * Finds student sessions by user ID and date range with relations eagerly
     * loaded.
     */
    public List<StudentSessionEntity> findByUserIdAndDateRange(final Long userId, final LocalDateTime start,
            final LocalDateTime end) {
        if (userId == null || start == null || end == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("StudentSession.findByUserAndDateRangeWithRelations",
                StudentSessionEntity.class);
        q.setParameter("u", userId);
        q.setParameter("s", start);
        q.setParameter("e", end);
        return q.getResultList();
    }

    /**
     * Finds student sessions by exercise ID and date range with relations eagerly
     * loaded.
     */
    public List<StudentSessionEntity> findByExerciseIdAndDateRange(final Long exerciseId, final LocalDateTime start,
            final LocalDateTime end) {
        if (exerciseId == null || start == null || end == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("StudentSession.findByExerciseAndDateRangeWithRelations",
                StudentSessionEntity.class);
        q.setParameter("e", exerciseId);
        q.setParameter("s", start);
        q.setParameter("en", end);
        return q.getResultList();
    }

    /**
     * Finds student sessions by completion status and date range with relations
     * eagerly loaded.
     */
    public List<StudentSessionEntity> findByCompletedAndDateRange(final Boolean completed, final LocalDateTime start,
            final LocalDateTime end) {
        if (completed == null || start == null || end == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("StudentSession.findByCompletedAndDateRangeWithRelations",
                StudentSessionEntity.class);
        q.setParameter("c", completed);
        q.setParameter("s", start);
        q.setParameter("e", end);
        return q.getResultList();
    }

    /**
     * Find sessions that started at or after the provided time with relations
     * eagerly
     * loaded.
     *
     * @param time lower bound start time (inclusive)
     * @return list of sessions starting at or after time, or empty list on null
     *         input
     */
    public List<StudentSessionEntity> findByStartTimeAfter(final LocalDateTime time) {
        if (time == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("StudentSession.findByStartTimeAfterWithRelations",
                StudentSessionEntity.class);
        q.setParameter("t", time);
        return q.getResultList();
    }

    /**
     * Find sessions that started before or at the provided time with relations
     * eagerly loaded.
     *
     * @param time upper bound start time (inclusive)
     * @return list of sessions starting before or at time, or empty list on null
     *         input
     */
    public List<StudentSessionEntity> findByStartTimeBefore(final LocalDateTime time) {
        if (time == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("StudentSession.findByStartTimeBeforeWithRelations",
                StudentSessionEntity.class);
        q.setParameter("e", time);
        return q.getResultList();
    }

    /**
     * Find sessions for a collection of user IDs with relations eagerly loaded.
     *
     * @param userIds list of user database ids
     * @return list of sessions, empty list if userIds is null or empty
     */
    public List<StudentSessionEntity> findByUserIdIn(final List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("StudentSession.findByUserIdInWithRelations",
                StudentSessionEntity.class);
        q.setParameter("ids", userIds);
        return q.getResultList();
    }

    /**
     * Find sessions with start times between the provided range with relations
     * eagerly loaded.
     *
     * @param start inclusive range start
     * @param end   inclusive range end
     * @return list of sessions in range or empty list on null inputs
     */
    public List<StudentSessionEntity> findByStartTimeBetween(final LocalDateTime start, final LocalDateTime end) {
        if (start == null || end == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("StudentSession.findByStartTimeBetweenWithRelations",
                StudentSessionEntity.class);
        q.setParameter("s", start);
        q.setParameter("e", end);
        return q.getResultList();
    }

    /**
     * Count sessions by completion flag.
     *
     * @param completed true to count completed sessions, false otherwise
     * @return count of matching sessions or 0 if completed is null
     */
    public long countByCompleted(final Boolean completed) {
        if (completed == null) {
            return 0L;
        }
        final var q = this.em.createNamedQuery("StudentSession.countByCompleted", Long.class);
        q.setParameter("c", completed);
        return q.getSingleResult();
    }

    /**
     * Count all student sessions.
     *
     * @return total number of sessions
     */
    public long countAll() {
        final var q = this.em.createNamedQuery("StudentSession.countAll", Long.class);
        return q.getSingleResult();
    }

    /**
     * Search sessions by a lower-cased term matching user or exercise fields with
     * relations eagerly loaded.
     *
     * @param lowerPattern lower-cased search pattern (e.g. "%term%")
     * @return matching sessions or empty list if pattern is null/empty
     */
    public List<StudentSessionEntity> searchByUserOrExerciseTerm(final String lowerPattern) {
        if (lowerPattern == null || lowerPattern.isEmpty()) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("StudentSession.searchByUserOrExerciseWithRelations",
                StudentSessionEntity.class);
        q.setParameter("p", lowerPattern);
        return q.getResultList();
    }

    /**
     * Count distinct active students (users with sessions) since the provided
     * time.
     *
     * @param time lower bound start time (inclusive)
     * @return count of distinct users with sessions starting at or after time
     */
    public long countActiveStudentsSince(final LocalDateTime time) {
        if (time == null) {
            return 0L;
        }
        final var q = this.em.createNamedQuery("StudentSession.countActiveStudents", Long.class);
        q.setParameter("t", time);
        return q.getSingleResult();
    }

    /**
     * Count sessions with start times between the provided range.
     *
     * @param start inclusive range start
     * @param end   inclusive range end
     * @return count of sessions in range
     */
    public long countByStartTimeBetween(final LocalDateTime start, final LocalDateTime end) {
        if (start == null || end == null) {
            return 0L;
        }
        final var q = this.em.createNamedQuery("StudentSession.countByStartTimeBetween", Long.class);
        q.setParameter("s", start);
        q.setParameter("e", end);
        return q.getSingleResult();
    }

    /**
     * Count sessions with start times in a half-open range [start, end).
     *
     * @param start inclusive range start
     * @param end   exclusive range end
     * @return count of sessions in range
     */
    public long countByStartTimeGreaterThanEqualAndStartTimeLessThan(final LocalDateTime start,
            final LocalDateTime end) {
        if (start == null || end == null) {
            return 0L;
        }
        final var q = this.em.createNamedQuery("StudentSession.countByStartTimeRangeHalfOpen", Long.class);
        q.setParameter("s", start);
        q.setParameter("e", end);
        return q.getSingleResult();
    }

    /**
     * Get problem category statistics using a GROUP BY JPQL query.
     *
     * @return list of object arrays containing [categoryName, count]
     */
    public List<Object[]> findProblemCategoryStats() {
        final var q = this.em.createNamedQuery("StudentSession.findProblemCategoryStats", Object[].class);
        return q.getResultList();
    }

    /**
     * Persist a new student session entity.
     *
     * @param session entity to persist; ignored if null
     */
    @Transactional
    public void persist(final StudentSessionEntity session) {
        if (session == null) {
            return;
        }
        this.em.persist(session);
    }

    /**
     * Delete a session by id.
     *
     * @param id primary key of session to remove
     * @return true if removed, false if id was null or entity not found
     */
    @Transactional
    public boolean deleteById(final Long id) {
        if (id == null) {
            return false;
        }
        final StudentSessionEntity e = this.em.find(StudentSessionEntity.class, id);
        if (e == null) {
            return false;
        }
        this.em.remove(e);
        return true;
    }
}
