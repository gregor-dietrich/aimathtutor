package de.vptr.aimathtutor.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import de.vptr.aimathtutor.entity.ExerciseEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * TODO: Class documentation.
 */
@ApplicationScoped
public class ExerciseRepository extends AbstractRepository {

    /**
     * TODO: Document findAllOrdered().
     */
    public List<ExerciseEntity> findAllOrdered() {
        return this.listNamed("Exercise.findAllOrdered", ExerciseEntity.class);
    }

    /**
     * TODO: Document findByIdOptional().
     */
    public Optional<ExerciseEntity> findByIdOptional(final Long id) {
        return Optional.ofNullable(this.findById(id));
    }

    /**
     * TODO: Document findById().
     */
    public ExerciseEntity findById(final Long id) {
        if (id == null) {
            return null;
        }
        return this.em.find(ExerciseEntity.class, id);
    }

    /**
     * TODO: Document findPublished().
     */
    public List<ExerciseEntity> findPublished() {
        return this.listNamed("Exercise.findPublished", ExerciseEntity.class);
    }

    /**
     * TODO: Document findByUserId().
     */
    public List<ExerciseEntity> findByUserId(final Long userId) {
        final var q = this.em.createNamedQuery("Exercise.findByUserId", ExerciseEntity.class);
        q.setParameter("u", userId);
        return q.getResultList();
    }

    /**
     * TODO: Document findByLessonId().
     */
    public List<ExerciseEntity> findByLessonId(final Long lessonId) {
        final var q = this.em.createNamedQuery("Exercise.findByLessonId", ExerciseEntity.class);
        q.setParameter("l", lessonId);
        return q.getResultList();
    }

    /**
     * TODO: Document findGraspableMathExercises().
     */
    public List<ExerciseEntity> findGraspableMathExercises() {
        return this.listNamed("Exercise.findGraspableEnabled", ExerciseEntity.class);
    }

    /**
     * TODO: Document findGraspableMathExercisesByLesson().
     */
    public List<ExerciseEntity> findGraspableMathExercisesByLesson(final Long lessonId) {
        final var q = this.em.createNamedQuery("Exercise.findGraspableByLesson", ExerciseEntity.class);
        q.setParameter("l", lessonId);
        return q.getResultList();
    }

    /**
     * TODO: Document persist().
     */
    @Transactional
    public ExerciseEntity persist(final ExerciseEntity exercise) {
        if (exercise == null) {
            return null;
        }
        this.em.persist(exercise);
        return exercise;
    }

    /**
     * TODO: Document deleteById().
     */
    @Transactional
    public boolean deleteById(final Long id) {
        final ExerciseEntity e = this.findById(id);
        if (e == null) {
            return false;
        }
        this.em.remove(e);
        return true;
    }

    /**
     * TODO: Document search().
     */
    public List<ExerciseEntity> search(final String query) {
        if (query == null || query.trim().isEmpty()) {
            return this.findAllOrdered();
        }
        final var searchTerm = "%" + query.trim().toLowerCase() + "%";
        final var q = this.em.createNamedQuery("Exercise.searchByTerm", ExerciseEntity.class);
        q.setParameter("s", searchTerm);
        return q.getResultList();
    }

    /**
     * TODO: Document findByDateRange().
     */
    public List<ExerciseEntity> findByDateRange(final LocalDateTime start, final LocalDateTime end) {
        final var q = this.em.createNamedQuery("Exercise.findByDateRange", ExerciseEntity.class);
        q.setParameter("s", start);
        q.setParameter("e", end);
        return q.getResultList();
    }
}
