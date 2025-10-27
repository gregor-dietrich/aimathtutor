package de.vptr.aimathtutor.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import de.vptr.aimathtutor.entity.CommentEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CommentRepository extends AbstractRepository {

    public List<CommentEntity> findAllOrdered() {
        return listNamed("Comment.findAllOrdered", CommentEntity.class);
    }

    /**
     * Fetch comments with related user, exercise and parentComment eagerly to avoid
     * lazy-loading in service layer.
     */
    public List<CommentEntity> findAllOrderedWithRelations() {
        return listNamed("Comment.findAllWithRelations", CommentEntity.class);
    }

    public Optional<CommentEntity> findByIdOptional(final Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.em.find(CommentEntity.class, id));
    }

    public Optional<CommentEntity> findByIdOptionalWithRelations(final Long id) {
        if (id == null) {
            return Optional.empty();
        }
        final var q = this.em.createNamedQuery("Comment.findByIdWithRelations", CommentEntity.class);
        q.setParameter("id", id);
        q.setMaxResults(1);
        return q.getResultStream().findFirst();
    }

    public CommentEntity findById(final Long id) {
        return this.em.find(CommentEntity.class, id);
    }

    public List<CommentEntity> findByExerciseId(final Long exerciseId) {
        final var q = this.em.createNamedQuery("Comment.findByExerciseId", CommentEntity.class);
        q.setParameter("e", exerciseId);
        return q.getResultList();
    }

    public List<CommentEntity> findByExerciseIdWithRelations(final Long exerciseId) {
        if (exerciseId == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("Comment.findByExerciseIdWithRelations", CommentEntity.class);
        q.setParameter("e", exerciseId);
        return q.getResultList();
    }

    public List<CommentEntity> findByUserId(final Long userId) {
        final var q = this.em.createNamedQuery("Comment.findByUserId", CommentEntity.class);
        q.setParameter("u", userId);
        return q.getResultList();
    }

    public List<CommentEntity> findByUserIdWithRelations(final Long userId) {
        if (userId == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("Comment.findByUserIdWithRelations", CommentEntity.class);
        q.setParameter("u", userId);
        return q.getResultList();
    }

    public List<CommentEntity> findRecentComments(final int limit) {
        final var q = this.em.createNamedQuery("Comment.findRecent", CommentEntity.class);
        q.setMaxResults(Math.max(0, limit));
        return q.getResultList();
    }

    public List<CommentEntity> findRecentCommentsWithRelations(final int limit) {
        if (limit <= 0) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("Comment.findRecentWithRelations", CommentEntity.class);
        q.setMaxResults(limit);
        return q.getResultList();
    }

    @Transactional
    public CommentEntity persist(final CommentEntity comment) {
        if (comment == null) {
            return null;
        }
        this.em.persist(comment);
        return comment;
    }

    @Transactional
    public boolean deleteById(final Long id) {
        final CommentEntity e = this.em.find(CommentEntity.class, id);
        if (e == null) {
            return false;
        }
        this.em.remove(e);
        return true;
    }

    public List<CommentEntity> findBySessionId(final String sessionId) {
        final var q = this.em.createNamedQuery("Comment.findBySessionId", CommentEntity.class);
        q.setParameter("s", sessionId);
        return q.getResultList();
    }

    public List<CommentEntity> findBySessionIdWithRelations(final String sessionId) {
        if (sessionId == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("Comment.findBySessionIdWithRelations", CommentEntity.class);
        q.setParameter("s", sessionId);
        return q.getResultList();
    }

    public List<CommentEntity> findReplies(final Long parentCommentId) {
        final var q = this.em.createNamedQuery("Comment.findReplies", CommentEntity.class);
        q.setParameter("p", parentCommentId);
        return q.getResultList();
    }

    public List<CommentEntity> findRepliesWithRelations(final Long parentCommentId) {
        if (parentCommentId == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("Comment.findRepliesWithRelations", CommentEntity.class);
        q.setParameter("p", parentCommentId);
        return q.getResultList();
    }

    public List<CommentEntity> findTopLevelByExercise(final Long exerciseId, final int page, final int pageSize) {
        final var q = this.em.createNamedQuery("Comment.findTopLevelByExercise", CommentEntity.class);
        q.setParameter("e", exerciseId);
        q.setFirstResult(page * pageSize);
        q.setMaxResults(pageSize);
        return q.getResultList();
    }

    public List<CommentEntity> findRepliesPaged(final Long parentId, final int page, final int pageSize) {
        final var q = this.em.createNamedQuery("Comment.findRepliesPaged", CommentEntity.class);
        q.setParameter("p", parentId);
        q.setFirstResult(page * pageSize);
        q.setMaxResults(pageSize);
        return q.getResultList();
    }

    public long countByUserSince(final Long userId, final LocalDateTime since) {
        final var q = this.em.createNamedQuery("Comment.countByUserSince", Long.class);
        q.setParameter("u", userId);
        q.setParameter("s", since);
        return q.getSingleResult();
    }

    public List<CommentEntity> search(final String searchTerm) {
        final var q = this.em.createNamedQuery("Comment.searchByTerm", CommentEntity.class);
        q.setParameter("s", searchTerm);
        return q.getResultList();
    }

    public List<CommentEntity> findByDateRange(final LocalDateTime start, final LocalDateTime end) {
        final var q = this.em.createNamedQuery("Comment.findByDateRange", CommentEntity.class);
        q.setParameter("s", start);
        q.setParameter("e", end);
        return q.getResultList();
    }

    public List<CommentEntity> findByStatus(final String status) {
        final var q = this.em.createNamedQuery("Comment.findByStatus", CommentEntity.class);
        q.setParameter("st", status);
        return q.getResultList();
    }

    public List<CommentEntity> findFlaggedComments(final Integer minFlags) {
        final var q = this.em.createNamedQuery("Comment.findFlaggedComments", CommentEntity.class);
        q.setParameter("m", minFlags);
        return q.getResultList();
    }
}
