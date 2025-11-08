package de.vptr.aimathtutor.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import de.vptr.aimathtutor.entity.CommentEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Repository for managing comment entities.
 * Provides database access and query operations for comments including
 * find by various criteria, threaded replies, pagination, and search
 * operations.
 */
@ApplicationScoped
public class CommentRepository extends AbstractRepository {

    /**
     * Retrieves all comments from the database in a defined order.
     *
     * @return a list of all {@link CommentEntity} objects ordered
     */
    public List<CommentEntity> findAllOrdered() {
        return this.listNamed("Comment.findAllOrdered", CommentEntity.class);
    }

    /**
     * Fetch comments with related user, exercise and parentComment eagerly to avoid
     * lazy-loading in service layer.
     */
    public List<CommentEntity> findAllOrderedWithRelations() {
        return this.listNamed("Comment.findAllWithRelations", CommentEntity.class);
    }

    /**
     * Retrieves an optional comment by its unique identifier.
     *
     * @param id the comment ID
     * @return an {@link Optional} containing the comment if found, empty otherwise
     */
    public Optional<CommentEntity> findByIdOptional(final Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.em.find(CommentEntity.class, id));
    }

    /**
     * Retrieves an optional comment by its unique identifier with related entities
     * eagerly loaded.
     * This prevents lazy-loading issues by fetching user, exercise, and parent
     * comment in one query.
     *
     * @param id the comment ID
     * @return an {@link Optional} containing the comment with relations if found,
     *         empty otherwise
     */
    public Optional<CommentEntity> findByIdOptionalWithRelations(final Long id) {
        if (id == null) {
            return Optional.empty();
        }
        final var q = this.em.createNamedQuery("Comment.findByIdWithRelations", CommentEntity.class);
        q.setParameter("id", id);
        q.setMaxResults(1);
        return q.getResultStream().findFirst();
    }

    /**
     * Retrieves a comment by its unique identifier.
     *
     * @param id the comment ID
     * @return the {@link CommentEntity} if found, null otherwise
     */
    public CommentEntity findById(final Long id) {
        return this.em.find(CommentEntity.class, id);
    }

    /**
     * Retrieves all comments for a specific exercise.
     *
     * @param exerciseId the exercise ID to filter by
     * @return a list of {@link CommentEntity} objects for the exercise
     */
    public List<CommentEntity> findByExerciseId(final Long exerciseId) {
        final var q = this.em.createNamedQuery("Comment.findByExerciseId", CommentEntity.class);
        q.setParameter("e", exerciseId);
        return q.getResultList();
    }

    /**
     * Retrieves all comments for a specific exercise with related entities eagerly
     * loaded.
     *
     * @param exerciseId the exercise ID to filter by
     * @return a list of {@link CommentEntity} objects with relations for the
     *         exercise
     */
    public List<CommentEntity> findByExerciseIdWithRelations(final Long exerciseId) {
        if (exerciseId == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("Comment.findByExerciseIdWithRelations", CommentEntity.class);
        q.setParameter("e", exerciseId);
        return q.getResultList();
    }

    /**
     * Retrieves all comments created by a specific user.
     *
     * @param userId the user ID to filter by
     * @return a list of {@link CommentEntity} objects created by the user
     */
    public List<CommentEntity> findByUserId(final Long userId) {
        final var q = this.em.createNamedQuery("Comment.findByUserId", CommentEntity.class);
        q.setParameter("u", userId);
        return q.getResultList();
    }

    /**
     * Retrieves all comments created by a specific user with related entities
     * eagerly loaded.
     *
     * @param userId the user ID to filter by
     * @return a list of {@link CommentEntity} objects with relations created by the
     *         user
     */
    public List<CommentEntity> findByUserIdWithRelations(final Long userId) {
        if (userId == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("Comment.findByUserIdWithRelations", CommentEntity.class);
        q.setParameter("u", userId);
        return q.getResultList();
    }

    /**
     * Retrieves the most recent comments from the database, up to a specified
     * limit.
     *
     * @param limit the maximum number of comments to retrieve (0 or negative
     *              returns empty list)
     * @return a list of recent {@link CommentEntity} objects
     */
    public List<CommentEntity> findRecentComments(final int limit) {
        final var q = this.em.createNamedQuery("Comment.findRecent", CommentEntity.class);
        q.setMaxResults(Math.max(0, limit));
        return q.getResultList();
    }

    /**
     * Retrieves the most recent comments with related entities eagerly loaded.
     *
     * @param limit the maximum number of comments to retrieve (0 or negative
     *              returns empty list)
     * @return a list of recent {@link CommentEntity} objects with relations
     */
    public List<CommentEntity> findRecentCommentsWithRelations(final int limit) {
        if (limit <= 0) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("Comment.findRecentWithRelations", CommentEntity.class);
        q.setMaxResults(limit);
        return q.getResultList();
    }

    /**
     * Persists a comment entity to the database.
     *
     * @param comment the comment to persist; null values are ignored
     * @return the persisted {@link CommentEntity}, or null if the input was null
     */
    @Transactional
    public CommentEntity persist(final CommentEntity comment) {
        if (comment == null) {
            return null;
        }
        this.em.persist(comment);
        return comment;
    }

    /**
     * Deletes a comment by its unique identifier.
     *
     * @param id the ID of the comment to delete
     * @return true if the comment was successfully deleted, false if not found
     */
    @Transactional
    public boolean deleteById(final Long id) {
        final CommentEntity e = this.em.find(CommentEntity.class, id);
        if (e == null) {
            return false;
        }
        this.em.remove(e);
        return true;
    }

    /**
     * Retrieves all comments associated with a specific student session.
     *
     * @param sessionId the session ID to filter by
     * @return a list of {@link CommentEntity} objects in the session
     */
    public List<CommentEntity> findBySessionId(final String sessionId) {
        final var q = this.em.createNamedQuery("Comment.findBySessionId", CommentEntity.class);
        q.setParameter("s", sessionId);
        return q.getResultList();
    }

    /**
     * Retrieves all comments associated with a specific student session with
     * related entities eagerly loaded.
     *
     * @param sessionId the session ID to filter by
     * @return a list of {@link CommentEntity} objects with relations in the session
     */
    public List<CommentEntity> findBySessionIdWithRelations(final String sessionId) {
        if (sessionId == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("Comment.findBySessionIdWithRelations", CommentEntity.class);
        q.setParameter("s", sessionId);
        return q.getResultList();
    }

    /**
     * Retrieves all reply comments to a specific parent comment.
     *
     * @param parentCommentId the ID of the parent comment to filter by
     * @return a list of {@link CommentEntity} objects that are replies to the
     *         parent
     */
    public List<CommentEntity> findReplies(final Long parentCommentId) {
        final var q = this.em.createNamedQuery("Comment.findReplies", CommentEntity.class);
        q.setParameter("p", parentCommentId);
        return q.getResultList();
    }

    /**
     * Retrieves all reply comments to a specific parent comment with related
     * entities eagerly loaded.
     *
     * @param parentCommentId the ID of the parent comment to filter by
     * @return a list of {@link CommentEntity} objects with relations that are
     *         replies to the parent
     */
    public List<CommentEntity> findRepliesWithRelations(final Long parentCommentId) {
        if (parentCommentId == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("Comment.findRepliesWithRelations", CommentEntity.class);
        q.setParameter("p", parentCommentId);
        return q.getResultList();
    }

    /**
     * Retrieves top-level (non-reply) comments for a specific exercise with
     * pagination.
     *
     * @param exerciseId the exercise ID to filter by
     * @param page       the page number (0-indexed)
     * @param pageSize   the number of comments per page
     * @return a paginated list of top-level {@link CommentEntity} objects for the
     *         exercise
     */
    public List<CommentEntity> findTopLevelByExercise(final Long exerciseId, final int page, final int pageSize) {
        final var q = this.em.createNamedQuery("Comment.findTopLevelByExercise", CommentEntity.class);
        q.setParameter("e", exerciseId);
        q.setFirstResult(page * pageSize);
        q.setMaxResults(pageSize);
        return q.getResultList();
    }

    /**
     * Retrieves reply comments with pagination.
     *
     * @param parentId the ID of the parent comment to filter by
     * @param page     the page number (0-indexed)
     * @param pageSize the number of replies per page
     * @return a paginated list of {@link CommentEntity} objects that are replies to
     *         the parent
     */
    public List<CommentEntity> findRepliesPaged(final Long parentId, final int page, final int pageSize) {
        final var q = this.em.createNamedQuery("Comment.findRepliesPaged", CommentEntity.class);
        q.setParameter("p", parentId);
        q.setFirstResult(page * pageSize);
        q.setMaxResults(pageSize);
        return q.getResultList();
    }

    /**
     * Counts comments created by a specific user since a given date and time.
     *
     * @param userId the user ID to filter by
     * @param since  the date and time threshold (inclusive)
     * @return the count of comments created by the user since the given date
     */
    public long countByUserSince(final Long userId, final LocalDateTime since) {
        final var q = this.em.createNamedQuery("Comment.countByUserSince", Long.class);
        q.setParameter("u", userId);
        q.setParameter("s", since);
        return q.getSingleResult();
    }

    /**
     * Searches for comments matching the given search term.
     *
     * @param searchTerm the search term to match against comment properties
     * @return a list of {@link CommentEntity} objects matching the search term
     */
    public List<CommentEntity> search(final String searchTerm) {
        final var q = this.em.createNamedQuery("Comment.searchByTerm", CommentEntity.class);
        q.setParameter("s", searchTerm);
        return q.getResultList();
    }

    /**
     * Retrieves comments within a specified date range.
     *
     * @param start the start date and time (inclusive)
     * @param end   the end date and time (inclusive)
     * @return a list of {@link CommentEntity} objects created within the date range
     */
    public List<CommentEntity> findByDateRange(final LocalDateTime start, final LocalDateTime end) {
        final var q = this.em.createNamedQuery("Comment.findByDateRange", CommentEntity.class);
        q.setParameter("s", start);
        q.setParameter("e", end);
        return q.getResultList();
    }

    /**
     * Retrieves comments with a specific status.
     *
     * @param status the status value to filter by (e.g., "active", "hidden",
     *               "flagged")
     * @return a list of {@link CommentEntity} objects with the specified status
     */
    public List<CommentEntity> findByStatus(final String status) {
        final var q = this.em.createNamedQuery("Comment.findByStatus", CommentEntity.class);
        q.setParameter("st", status);
        return q.getResultList();
    }

    /**
     * Retrieves comments that have been flagged by users, meeting a minimum flag
     * count threshold.
     *
     * @param minFlags the minimum number of flags a comment must have to be
     *                 included in results
     * @return a list of {@link CommentEntity} objects with at least the specified
     *         number of flags
     */
    public List<CommentEntity> findFlaggedComments(final Integer minFlags) {
        final var q = this.em.createNamedQuery("Comment.findFlaggedComments", CommentEntity.class);
        q.setParameter("m", minFlags);
        return q.getResultList();
    }
}
