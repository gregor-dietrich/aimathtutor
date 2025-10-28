package de.vptr.aimathtutor.repository;

import java.time.LocalDateTime;
import java.util.List;

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
     * Find all sessions for a given user id.
     *
     * @param userId database id of the user
     * @return list of sessions, empty list if userId is null or none found
     */
    public List<StudentSessionEntity> findByUserId(final Long userId) {
        if (userId == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("StudentSession.findByUserId", StudentSessionEntity.class);
        q.setParameter("u", userId);
        return q.getResultList();
    }

    /**
     * Find all sessions for a given exercise id.
     *
     * @param exerciseId database id of the exercise
     * @return list of sessions, empty list if exerciseId is null or none found
     */
    public List<StudentSessionEntity> findByExerciseId(final Long exerciseId) {
        if (exerciseId == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("StudentSession.findByExerciseId", StudentSessionEntity.class);
        q.setParameter("e", exerciseId);
        return q.getResultList();
    }

    /**
     * List all student sessions ordered by start time (named query
     * "StudentSession.findAllOrdered").
     *
     * @return list of all student sessions
     */
    public List<StudentSessionEntity> findAll() {
        return this.listNamed("StudentSession.findAllOrdered", StudentSessionEntity.class);
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
        return this.em.find(StudentSessionEntity.class, id);
    }

    /**
     * Find sessions by user and exercise in a single DB query
     */
    public List<StudentSessionEntity> findByUserIdAndExerciseId(final Long userId, final Long exerciseId) {
        if (userId == null || exerciseId == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("StudentSession.findByUserAndExercise", StudentSessionEntity.class);
        q.setParameter("u", userId);
        q.setParameter("e", exerciseId);
        return q.getResultList();
    }

    /**
     * Finds student sessions by user ID and date range.
     */
    public List<StudentSessionEntity> findByUserIdAndDateRange(final Long userId, final LocalDateTime start,
            final LocalDateTime end) {
        if (userId == null || start == null || end == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("StudentSession.findByUserAndDateRange", StudentSessionEntity.class);
        q.setParameter("u", userId);
        q.setParameter("s", start);
        q.setParameter("e", end);
        return q.getResultList();
    }

    /**
     * Finds student sessions by exercise ID and date range.
     */
    public List<StudentSessionEntity> findByExerciseIdAndDateRange(final Long exerciseId, final LocalDateTime start,
            final LocalDateTime end) {
        if (exerciseId == null || start == null || end == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("StudentSession.findByExerciseAndDateRange", StudentSessionEntity.class);
        q.setParameter("e", exerciseId);
        q.setParameter("s", start);
        q.setParameter("en", end);
        return q.getResultList();
    }

    /**
     * Finds student sessions by completion status and date range.
     */
    public List<StudentSessionEntity> findByCompletedAndDateRange(final Boolean completed, final LocalDateTime start,
            final LocalDateTime end) {
        if (completed == null || start == null || end == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("StudentSession.findByCompletedAndDateRange",
                StudentSessionEntity.class);
        q.setParameter("c", completed);
        q.setParameter("s", start);
        q.setParameter("e", end);
        return q.getResultList();
    }

    /**
     * Find sessions that started after the provided time.
     *
     * @param time lower bound start time (exclusive)
     * @return list of sessions starting after time, or empty list on null input
     */
    public List<StudentSessionEntity> findByStartTimeAfter(final LocalDateTime time) {
        if (time == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("StudentSession.findByStartTimeAfter", StudentSessionEntity.class);
        q.setParameter("t", time);
        return q.getResultList();
    }

    /**
     * Find sessions with start times between the provided range.
     *
     * @param start inclusive range start
     * @param end   inclusive range end
     * @return list of sessions in range or empty list on null inputs
     */
    public List<StudentSessionEntity> findByStartTimeBetween(final LocalDateTime start, final LocalDateTime end) {
        if (start == null || end == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("StudentSession.findByStartTimeBetween", StudentSessionEntity.class);
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
     * Search sessions by a lower-cased term matching user or exercise fields.
     *
     * @param lowerPattern lower-cased search pattern (e.g. "%term%")
     * @return matching sessions or empty list if pattern is null/empty
     */
    public List<StudentSessionEntity> searchByUserOrExerciseTerm(final String lowerPattern) {
        if (lowerPattern == null || lowerPattern.isEmpty()) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("StudentSession.searchByUserOrExercise", StudentSessionEntity.class);
        q.setParameter("p", lowerPattern);
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
