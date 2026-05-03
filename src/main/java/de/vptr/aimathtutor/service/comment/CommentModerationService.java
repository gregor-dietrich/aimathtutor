package de.vptr.aimathtutor.service.comment;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.vptr.aimathtutor.dto.CommentDto.CommentStatus;
import de.vptr.aimathtutor.entity.CommentEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.repository.CommentRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Service for moderating comments (hide, show, restore, delete).
 */
@ApplicationScoped
public class CommentModerationService {

    private static final Logger LOG = LoggerFactory.getLogger(CommentModerationService.class);

    @Inject
    CommentRepository commentRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    CommentPermissionService commentPermissionService;

    /**
     * Moderate a comment (hide/unhide/restore/delete).
     *
     * @param commentId   the comment ID
     * @param action      the moderation action: "HIDE", "SHOW", "RESTORE", "DELETE"
     * @param moderatorId the moderator user ID
     * @param reason      the reason for moderation
     */
    @Transactional
    public void moderateComment(
            final Long commentId,
            final String action,
            final Long moderatorId,
            final String reason) {
        if (reason != null && reason.length() > 500) {
            throw new ValidationException("Moderation reason must be <= 500 characters");
        }
        LOG.info("Moderating comment: commentId={}, action={}, moderatorId={}, reasonLength={}", commentId, action,
                moderatorId, reason != null ? reason.length() : 0);

        final CommentEntity comment = this.commentRepository.findById(commentId);
        if (comment == null) {
            LOG.warn("Moderate comment failed: comment not found commentId={}, moderatorId={}", commentId, moderatorId);
            throw new WebApplicationException("Comment not found", Response.Status.NOT_FOUND);
        }

        final UserEntity moderator = this.userRepository.findById(moderatorId);
        if (!this.commentPermissionService.isModerator(moderator)) {
            LOG.warn("Moderate comment unauthorized: commentId={}, moderatorId={}", commentId, moderatorId);
            throw new WebApplicationException("Only moderators can perform moderation",
                    Response.Status.FORBIDDEN);
        }

        if (action == null) {
            LOG.warn("Null moderation action: commentId={}, moderatorId={}", commentId, moderatorId);
            throw new ValidationException("Moderation action is required");
        }

        switch (action.toUpperCase()) {
            case "HIDE":
                comment.status = CommentStatus.HIDDEN;
                comment.moderationReason = reason;
                comment.moderator = moderator;
                comment.moderationAction = action.toUpperCase();
                comment.moderatedAt = LocalDateTime.now();
                LOG.info("Comment hidden by moderator: commentId={}, moderatorId={}", commentId, moderatorId);
                break;
            case "SHOW":
                comment.status = CommentStatus.VISIBLE;
                comment.flagsCount = 0;
                comment.deletedBy = null;
                comment.deletedAt = null;
                comment.moderationReason = reason;
                comment.moderator = moderator;
                comment.moderationAction = action.toUpperCase();
                comment.moderatedAt = LocalDateTime.now();
                LOG.info("Comment shown by moderator: commentId={}, moderatorId={}", commentId, moderatorId);
                break;
            case "RESTORE":
                // Restore a deleted comment (same as SHOW) and clear flags
                comment.status = CommentStatus.VISIBLE;
                comment.flagsCount = 0;
                comment.deletedBy = null;
                comment.deletedAt = null;
                comment.moderationReason = reason;
                comment.moderator = moderator;
                comment.moderationAction = action.toUpperCase();
                comment.moderatedAt = LocalDateTime.now();
                LOG.info("Comment restored by moderator: commentId={}, moderatorId={}", commentId, moderatorId);
                break;
            case "DELETE":
                comment.status = CommentStatus.DELETED;
                comment.deletedBy = moderator;
                comment.deletedAt = LocalDateTime.now();
                comment.moderationReason = reason;
                comment.moderator = moderator;
                comment.moderationAction = action.toUpperCase();
                comment.moderatedAt = LocalDateTime.now();
                LOG.info("Comment deleted by moderator: commentId={}, moderatorId={}", commentId, moderatorId);
                break;
            default:
                LOG.warn("Invalid moderation action: action={}, commentId={}, moderatorId={}", action, commentId,
                        moderatorId);
                throw new ValidationException("Invalid moderation action: " + action);
        }

        this.commentRepository.persist(comment);
    }
}
