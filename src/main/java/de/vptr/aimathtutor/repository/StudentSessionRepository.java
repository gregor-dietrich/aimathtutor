package de.vptr.aimathtutor.repository;

import java.time.LocalDateTime;
import java.util.List;

import de.vptr.aimathtutor.entity.StudentSessionEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * TODO: Class documentation.
 */
@ApplicationScoped
public class StudentSessionRepository extends AbstractRepository {

    /**
     * TODO: Document findBySessionId().
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
     * TODO: Document findByUserId().
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
     * TODO: Document findByExerciseId().
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
     * TODO: Document findAll().
     */
    public List<StudentSessionEntity> findAll() {
        return this.listNamed("StudentSession.findAllOrdered", StudentSessionEntity.class);
    }

    /**
     * TODO: Document findById().
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
     * TODO: Document findByStartTimeAfter().
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
     * TODO: Document findByStartTimeBetween().
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
     * TODO: Document countByCompleted().
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
     * TODO: Document countAll().
     */
    public long countAll() {
        final var q = this.em.createNamedQuery("StudentSession.countAll", Long.class);
        return q.getSingleResult();
    }

    /**
     * TODO: Document searchByUserOrExerciseTerm().
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
     * TODO: Document persist().
     */
    @Transactional
    public void persist(final StudentSessionEntity session) {
        if (session == null) {
            return;
        }
        this.em.persist(session);
    }

    /**
     * TODO: Document deleteById().
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
