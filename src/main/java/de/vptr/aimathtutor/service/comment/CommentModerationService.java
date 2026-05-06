package de.vptr.aimathtutor.service.comment;

import java.time.LocalDateTime;
import java.util.Locale;

import org.jboss.logging.Logger;

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

    private static final Logger LOG = Logger.getLogger(CommentModerationService.class);

    @Inject
    CommentRepository commentRepository;

    @Inject
    UserRepository userRepository;

    /**
     * Moderate a comment (hide/unhide/restore/delete).
     *
     * @param commentPublicId the comment public ID
     * @param action          the moderation action: "HIDE", "SHOW", "RESTORE",
     *                        "DELETE"
     * @param moderatorId     the moderator user ID
     * @param reason          the reason for moderation
     */
    @Transactional
    public void moderateComment(
            final String commentPublicId,
            final String action,
            final Long moderatorId,
            final String reason) {
        if (reason != null && reason.length() > 500) {
            throw new ValidationException("Moderation reason must be <= 500 characters");
        }
        LOG.infof("Moderating comment: commentPublicId=%s, action=%s, moderatorId=%s, reasonLength=%s",  commentPublicId, 
                action, 
                moderatorId,  reason != null ? reason.length() : 0);

        final CommentEntity comment = this.commentRepository.findByPublicId(commentPublicId).orElse(null);
        if (comment == null) {
            LOG.warnf("Moderate comment failed: comment not found commentPublicId=%s, moderatorId=%s",  commentPublicId, 
                    moderatorId);
            throw new WebApplicationException("Comment not found", Response.Status.NOT_FOUND);
        }

        if (moderatorId == null) {
            LOG.warnf("Moderate comment failed: moderatorId is null commentPublicId=%s",  commentPublicId);
            throw new WebApplicationException("Moderator ID is required", Response.Status.BAD_REQUEST);
        }

        final UserEntity moderator = this.userRepository.findById(moderatorId);
        if (moderator == null) {
            LOG.warnf("Moderate comment failed: moderator not found commentPublicId=%s, moderatorId=%s",  commentPublicId,
                    moderatorId);
            throw new WebApplicationException("Moderator not found", Response.Status.BAD_REQUEST);
        }

        if (action == null) {
            LOG.warnf("Null moderation action: commentPublicId=%s, moderatorId=%s",  commentPublicId,  moderatorId);
            throw new ValidationException("Moderation action is required");
        }

        final String normalizedAction = action.toUpperCase(Locale.ROOT);
        switch (normalizedAction) {
            case "HIDE":
                comment.status = CommentStatus.HIDDEN;
                comment.moderationReason = reason;
                comment.moderator = moderator;
                comment.moderationAction = normalizedAction;
                comment.moderatedAt = LocalDateTime.now();
                LOG.infof("Comment hidden by moderator: commentPublicId=%s, moderatorId=%s",  commentPublicId,
                        moderatorId);
                break;
            case "SHOW":
                comment.status = CommentStatus.VISIBLE;
                comment.flagsCount = 0;
                comment.deletedBy = null;
                comment.deletedAt = null;
                comment.moderationReason = reason;
                comment.moderator = moderator;
                comment.moderationAction = normalizedAction;
                comment.moderatedAt = LocalDateTime.now();
                LOG.infof("Comment shown by moderator: commentPublicId=%s, moderatorId=%s",  commentPublicId,
                        moderatorId);
                break;
            case "RESTORE":
                // Restore a deleted comment (same as SHOW) and clear flags
                comment.status = CommentStatus.VISIBLE;
                comment.flagsCount = 0;
                comment.deletedBy = null;
                comment.deletedAt = null;
                comment.moderationReason = reason;
                comment.moderator = moderator;
                comment.moderationAction = normalizedAction;
                comment.moderatedAt = LocalDateTime.now();
                LOG.infof("Comment restored by moderator: commentPublicId=%s, moderatorId=%s",  commentPublicId,
                        moderatorId);
                break;
            case "DELETE":
                comment.status = CommentStatus.DELETED;
                comment.deletedBy = moderator;
                comment.deletedAt = LocalDateTime.now();
                comment.moderationReason = reason;
                comment.moderator = moderator;
                comment.moderationAction = normalizedAction;
                comment.moderatedAt = LocalDateTime.now();
                LOG.infof("Comment deleted by moderator: commentPublicId=%s, moderatorId=%s",  commentPublicId,
                        moderatorId);
                break;
            default:
                LOG.warnf("Invalid moderation action: action=%s, commentPublicId=%s, moderatorId=%s",  action,
                        commentPublicId,
                        moderatorId);
                throw new ValidationException("Invalid moderation action: " + action);
        }

        this.commentRepository.persist(comment);
    }
}
