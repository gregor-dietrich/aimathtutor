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
 * TODO: Class documentation.
 */
@ApplicationScoped
public class CommentFlagRepository {

    @Inject
    EntityManager em;

    /**
     * TODO: Document hasUserFlaggedComment().
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
     * TODO: Document persist().
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
     * TODO: Document findByIdOptional().
     */
    public Optional<CommentFlagEntity> findByIdOptional(final Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.em.find(CommentFlagEntity.class, id));
    }
}
