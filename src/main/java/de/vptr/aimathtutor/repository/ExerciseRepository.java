package de.vptr.aimathtutor.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import de.vptr.aimathtutor.entity.ExerciseEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Repository for managing exercise entities.
 * Provides database access and query operations for exercises including
 * find by various criteria, Graspable Math exercises, and search operations.
 */
@ApplicationScoped
public class ExerciseRepository extends AbstractRepository {

    /**
     * Retrieves all exercises from the database in a defined order.
     *
     * @return a list of all {@link ExerciseEntity} objects ordered
     */
    public List<ExerciseEntity> findAllOrdered() {
        return this.listNamed("Exercise.findAllOrdered", ExerciseEntity.class);
    }

    /**
     * Retrieves an optional exercise by its unique identifier.
     *
     * @param id the exercise ID
     * @return an {@link Optional} containing the exercise if found, empty otherwise
     */
    public Optional<ExerciseEntity> findByIdOptional(final Long id) {
        return Optional.ofNullable(this.findById(id));
    }

    /**
     * Retrieves an exercise by its unique identifier.
     *
     * @param id the exercise ID
     * @return the {@link ExerciseEntity} if found, null otherwise
     */
    public ExerciseEntity findById(final Long id) {
        if (id == null) {
            return null;
        }
        return this.em.find(ExerciseEntity.class, id);
    }

    /**
     * Retrieves all published exercises from the database.
     *
     * @return a list of all published {@link ExerciseEntity} objects
     */
    public List<ExerciseEntity> findPublished() {
        return this.listNamed("Exercise.findPublished", ExerciseEntity.class);
    }

    /**
     * Retrieves all exercises created by a specific user.
     *
     * @param userId the user ID to filter by
     * @return a list of {@link ExerciseEntity} objects created by the user
     */
    public List<ExerciseEntity> findByUserId(final Long userId) {
        final var q = this.em.createNamedQuery("Exercise.findByUserId", ExerciseEntity.class);
        q.setParameter("u", userId);
        return q.getResultList();
    }

    /**
     * Retrieves all exercises belonging to a specific lesson.
     *
     * @param lessonId the lesson ID to filter by
     * @return a list of {@link ExerciseEntity} objects in the lesson
     */
    public List<ExerciseEntity> findByLessonId(final Long lessonId) {
        final var q = this.em.createNamedQuery("Exercise.findByLessonId", ExerciseEntity.class);
        q.setParameter("l", lessonId);
        return q.getResultList();
    }

    /**
     * Retrieves all exercises that use Graspable Math.
     *
     * @return a list of Graspable Math enabled {@link ExerciseEntity} objects
     */
    public List<ExerciseEntity> findGraspableMathExercises() {
        return this.listNamed("Exercise.findGraspableEnabled", ExerciseEntity.class);
    }

    /**
     * Retrieves all Graspable Math exercises in a specific lesson.
     *
     * @param lessonId the lesson ID to filter by
     * @return a list of Graspable Math enabled {@link ExerciseEntity} objects in
     *         the lesson
     */
    public List<ExerciseEntity> findGraspableMathExercisesByLesson(final Long lessonId) {
        final var q = this.em.createNamedQuery("Exercise.findGraspableByLesson", ExerciseEntity.class);
        q.setParameter("l", lessonId);
        return q.getResultList();
    }

    /**
     * Persists an exercise entity to the database.
     *
     * @param exercise the exercise to persist; null values are ignored
     * @return the persisted {@link ExerciseEntity}, or null if the input was null
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
     * Deletes an exercise by its unique identifier.
     *
     * @param id the ID of the exercise to delete
     * @return true if the exercise was successfully deleted, false if not found
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
     * Searches for exercises matching the given query term.
     *
     * @param query the search query to match against exercise properties;
     *              if null or empty, returns all exercises ordered
     * @return a list of {@link ExerciseEntity} objects matching the search query
     */
    public List<ExerciseEntity> search(final String query) {
        if (query == null || query.isBlank()) {
            return this.findAllOrdered();
        }
        final var searchTerm = "%" + query.trim().toLowerCase() + "%";
        final var q = this.em.createNamedQuery("Exercise.searchByTerm", ExerciseEntity.class);
        q.setParameter("s", searchTerm);
        return q.getResultList();
    }

    /**
     * Retrieves exercises within a specified date range.
     *
     * @param start the start date and time (inclusive)
     * @param end   the end date and time (inclusive)
     * @return a list of {@link ExerciseEntity} objects created within the date
     *         range
     */
    public List<ExerciseEntity> findByDateRange(final LocalDateTime start, final LocalDateTime end) {
        final var q = this.em.createNamedQuery("Exercise.findByDateRange", ExerciseEntity.class);
        q.setParameter("s", start);
        q.setParameter("e", end);
        return q.getResultList();
    }
}
