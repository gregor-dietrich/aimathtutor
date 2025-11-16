package de.vptr.aimathtutor.repository;

import java.util.List;
import java.util.Optional;

import de.vptr.aimathtutor.entity.LessonEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

/**
 * Repository for managing lesson entities.
 * Provides database access and query operations for lessons including
 * find by ID, hierarchical navigation, and search operations.
 */
@ApplicationScoped
public class LessonRepository {

    @Inject
    EntityManager em;

    /**
     * Retrieves a lesson by its unique identifier.
     *
     * @param id the lesson ID
     * @return the {@link LessonEntity} if found, null otherwise
     */
    public LessonEntity findById(final Long id) {
        if (id == null) {
            return null;
        }
        return this.em.find(LessonEntity.class, id);
    }

    /**
     * Retrieves an optional lesson by its unique identifier.
     *
     * @param id the lesson ID
     * @return an {@link Optional} containing the lesson if found, empty otherwise
     */
    public Optional<LessonEntity> findByIdOptional(final Long id) {
        return Optional.ofNullable(this.findById(id));
    }

    /**
     * Retrieves all lessons from the database in a defined order (descending by
     * ID).
     *
     * @return a list of all {@link LessonEntity} objects ordered
     */
    public List<LessonEntity> findAllOrdered() {
        final TypedQuery<LessonEntity> q = this.em.createQuery("FROM LessonEntity ORDER BY id DESC",
                LessonEntity.class);
        return q.getResultList();
    }

    /**
     * Retrieves all root-level lessons (lessons with no parent).
     *
     * @return a list of root {@link LessonEntity} objects ordered descending by ID
     */
    public List<LessonEntity> findRootLessons() {
        final TypedQuery<LessonEntity> q = this.em.createQuery(
                "FROM LessonEntity WHERE parent IS NULL ORDER BY id DESC",
                LessonEntity.class);
        return q.getResultList();
    }

    /**
     * Retrieves all lessons that are children of a specific parent lesson.
     *
     * @param parentId the ID of the parent lesson to filter by
     * @return a list of child {@link LessonEntity} objects ordered descending by ID
     */
    public List<LessonEntity> findByParentId(final Long parentId) {
        final TypedQuery<LessonEntity> q = this.em.createQuery(
                "FROM LessonEntity WHERE parent.id = :p ORDER BY id DESC",
                LessonEntity.class);
        q.setParameter("p", parentId);
        return q.getResultList();
    }

    /**
     * Searches for lessons matching the given search term in their names.
     *
     * @param searchTerm the search term to match against lesson names;
     *                   if null or empty, returns all lessons ordered
     * @return a list of {@link LessonEntity} objects matching the search term
     */
    public List<LessonEntity> search(final String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return this.findAllOrdered();
        }
        final var pattern = "%" + searchTerm.trim().toLowerCase() + "%";
        final TypedQuery<LessonEntity> q = this.em.createQuery("FROM LessonEntity WHERE LOWER(name) LIKE :s",
                LessonEntity.class);
        q.setParameter("s", pattern);
        return q.getResultList();
    }

    /**
     * Deletes a lesson by its unique identifier.
     *
     * @param id the ID of the lesson to delete
     * @return true if the lesson was successfully deleted, false if not found
     */
    @Transactional
    public boolean deleteById(final Long id) {
        final LessonEntity e = this.findById(id);
        if (e == null) {
            return false;
        }
        this.em.remove(e);
        return true;
    }

    /**
     * Persists a lesson entity to the database.
     *
     * @param lesson the lesson to persist; null values are ignored
     * @return the persisted {@link LessonEntity}, or null if the input was null
     */
    @Transactional
    public LessonEntity persist(final LessonEntity lesson) {
        if (lesson == null) {
            return null;
        }
        this.em.persist(lesson);
        return lesson;
    }

}
