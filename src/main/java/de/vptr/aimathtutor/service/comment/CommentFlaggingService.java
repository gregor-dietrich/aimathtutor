package de.vptr.aimathtutor.service.comment;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.vptr.aimathtutor.dto.CommentViewDto;
import de.vptr.aimathtutor.entity.CommentEntity;
import de.vptr.aimathtutor.repository.CommentFlagRepository;
import de.vptr.aimathtutor.repository.CommentRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.util.AppConstants;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Service for flagging comments and retrieving flagged comments.
 */
@ApplicationScoped
public class CommentFlaggingService {

    private static final Logger LOG = LoggerFactory.getLogger(CommentFlaggingService.class);

    @Inject
    CommentRepository commentRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    CommentFlagRepository commentFlagRepository;

    /**
     * Flag a comment for moderation review.
     *
     * @param commentId the comment ID
     * @param flaggerId the user ID of the flagger
     * @param reason    the reason for flagging
     */
    @Transactional
    public void flagComment(final Long commentId, final Long flaggerId, final String reason) {
        LOG.info("Flagging comment: commentId={}, flaggerId={}, reasonProvided={}", commentId, flaggerId,
                reason != null && !reason.isBlank());

        final CommentEntity comment = this.commentRepository.findById(commentId);
        if (comment == null) {
            LOG.warn("Flag comment failed: comment not found commentId={}, flaggerId={}", commentId, flaggerId);
            throw new WebApplicationException("Comment not found", Response.Status.NOT_FOUND);
        }

        // Prevent self-flagging
        if (comment.user != null && comment.user.id.equals(flaggerId)) {
            LOG.warn("Self-flag attempt: commentId={}, flaggerId={}", commentId, flaggerId);
            throw new WebApplicationException("Cannot flag your own comment", Response.Status.BAD_REQUEST);
        }

        // Create flag record via repository (repository handles duplicate-check)
        final var flagger = this.userRepository.findById(flaggerId);
        if (flagger == null) {
            throw new WebApplicationException("User not found", Response.Status.NOT_FOUND);
        }
        this.commentFlagRepository.createFlag(comment, flagger);

        // Increment flag count
        comment.flagsCount = (comment.flagsCount != null ? comment.flagsCount : 0) + 1;

        // If flagged enough times, auto-hide
        if (comment.flagsCount >= AppConstants.COMMENT_AUTO_HIDE_THRESHOLD) {
            comment.status = "HIDDEN";
            LOG.warn("Comment auto-hidden due to flags: commentId={}, flagCount={}", commentId, comment.flagsCount);
        }

        this.commentRepository.persist(comment);
        LOG.info("Comment flagged: commentId={}, flaggerId={}, newFlagCount={}", commentId, flaggerId,
                comment.flagsCount);
    }

    /**
     * Find comments with N or more flags (for moderation).
     *
     * @param minFlags the minimum number of flags
     * @return a list of flagged {@link CommentViewDto}s
     */
    @Transactional
    public List<CommentViewDto> findFlaggedComments(final Integer minFlags) {
        final List<CommentEntity> comments = this.commentRepository.findFlaggedComments(minFlags);
        return comments.stream()
                .map(CommentViewDto::new)
                .collect(Collectors.toList());
    }
}
