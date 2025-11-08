package de.vptr.aimathtutor.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import de.vptr.aimathtutor.entity.CommentEntity;
import de.vptr.aimathtutor.entity.CommentFlagEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Repository for managing comment flag entities (reports/flags).
 * Provides database access for comment moderation including
 * flag creation, lookup, and user flag tracking operations.
 */
@ApplicationScoped
public class CommentFlagRepository {

    @Inject
    EntityManager em;

    /**
     * Checks if a specific user has already flagged a given comment.
     *
     * @param commentId the ID of the comment to check
     * @param userId    the ID of the user who may have flagged it
     * @return true if the user has flagged the comment, false otherwise;
     *         returns false if commentId or userId is null
     */
    public boolean hasUserFlaggedComment(final Long commentId, final Long userId) {
        if (commentId == null || userId == null) {
            return false;
        }
        final var q = this.em.createNamedQuery("CommentFlag.countByCommentAndFlagger", Long.class);
        q.setParameter("c", commentId);
        q.setParameter("u", userId);
        return q.getSingleResult() > 0;
    }

    /**
     * Persists a comment flag entity to the database.
     *
     * @param flag the comment flag to persist; null values are ignored
     */
    @Transactional
    public void persist(final CommentFlagEntity flag) {
        if (flag == null) {
            return;
        }
        this.em.persist(flag);
    }

    /**
     * Create and persist a new flag for a comment by a user.
     * Throws WebApplicationException with BAD_REQUEST if the user already flagged
     * the comment.
     */
    @Transactional
    public CommentFlagEntity createFlag(final CommentEntity comment, final UserEntity flagger) {
        if (comment == null || flagger == null) {
            throw new WebApplicationException("Comment or flagger not provided", Response.Status.BAD_REQUEST);
        }

        if (this.hasUserFlaggedComment(comment.id, flagger.id)) {
            throw new WebApplicationException("You have already flagged this comment", Response.Status.BAD_REQUEST);
        }

        final var flag = new CommentFlagEntity();
        flag.comment = comment;
        flag.flagger = flagger;
        flag.created = LocalDateTime.now();
        this.em.persist(flag);
        return flag;
    }

    /**
     * Retrieves an optional comment flag by its unique identifier.
     *
     * @param id the comment flag ID
     * @return an {@link Optional} containing the flag if found, empty otherwise;
     *         returns empty if id is null
     */
    public Optional<CommentFlagEntity> findByIdOptional(final Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.em.find(CommentFlagEntity.class, id));
    }
}
