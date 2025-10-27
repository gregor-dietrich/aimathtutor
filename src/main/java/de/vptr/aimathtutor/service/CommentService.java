package de.vptr.aimathtutor.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.vptr.aimathtutor.dto.CommentDto;
import de.vptr.aimathtutor.dto.CommentViewDto;
import de.vptr.aimathtutor.entity.CommentEntity;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.event.CommentCreatedEvent;
import de.vptr.aimathtutor.repository.CommentFlagRepository;
import de.vptr.aimathtutor.repository.CommentRepository;
import de.vptr.aimathtutor.repository.ExerciseRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Service for managing comments: creation, editing, deletion, listing and
 * moderation.
 */
@ApplicationScoped
public class CommentService {

    private static final Logger LOG = LoggerFactory.getLogger(CommentService.class);

    private static final long RATE_LIMIT_WINDOW_SECONDS = 5;
    private static final int RATE_LIMIT_DAILY = 200;

    @Inject
    UserService userService;

    @Inject
    Event<CommentCreatedEvent> commentCreatedEvent;

    @Inject
    CommentRepository commentRepository;

    @Inject
    ExerciseRepository exerciseRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    CommentFlagRepository commentFlagRepository;

    /**
     * TODO: Document getAllComments().
     */
    @Transactional
    public List<CommentViewDto> getAllComments() {
        final List<CommentEntity> comments = this.commentRepository.findAllOrderedWithRelations();
        return comments.stream()
                .map(CommentViewDto::new)
                .collect(Collectors.toList());
    }

    /**
     * TODO: Document findById().
     */
    @Transactional
    public Optional<CommentViewDto> findById(final Long id) {
        final Optional<CommentEntity> comment = this.commentRepository.findByIdOptionalWithRelations(id);
        if (comment.isPresent()) {
            final CommentEntity entity = comment.get();
            return Optional.of(new CommentViewDto(entity));
        }
        return Optional.empty();
    }

    /**
     * TODO: Document findByExerciseId().
     */
    @Transactional
    public List<CommentViewDto> findByExerciseId(final Long exerciseId) {
        final List<CommentEntity> comments = this.commentRepository.findByExerciseIdWithRelations(exerciseId);
        return comments.stream()
                .map(CommentViewDto::new)
                .collect(Collectors.toList());
    }

    /**
     * TODO: Document findByUserId().
     */
    @Transactional
    public List<CommentViewDto> findByUserId(final Long userId) {
        final List<CommentEntity> comments = this.commentRepository.findByUserIdWithRelations(userId);
        return comments.stream()
                .map(CommentViewDto::new)
                .collect(Collectors.toList());
    }

    /**
     * TODO: Document findRecentComments().
     */
    @Transactional
    public List<CommentViewDto> findRecentComments(final int limit) {
        final List<CommentEntity> comments = this.commentRepository.findRecentCommentsWithRelations(limit);
        return comments.stream()
                .map(CommentViewDto::new)
                .collect(Collectors.toList());
    }

    /**
     * TODO: Document createComment().
     */
    @Transactional
    public CommentViewDto createComment(final CommentEntity comment, final String currentUsername) {
        // Validate content is provided for creation
        if (comment.content == null || comment.content.trim().isEmpty()) {
            throw new ValidationException("Content is required for creating a comment");
        }

        // Validate exercise exists
        final ExerciseEntity existingExercise = this.exerciseRepository
                .findById(comment.exercise != null ? comment.exercise.id : null);
        if (existingExercise == null) {
            throw new WebApplicationException(
                    "Exercise with ID " + (comment.exercise != null ? comment.exercise.id : null) + " does not exist.",
                    Response.Status.BAD_REQUEST);
        }

        // Validate exercise is published
        if (existingExercise.published == null || !existingExercise.published) {
            throw new WebApplicationException("Cannot add comment to an unpublished exercise.",
                    Response.Status.BAD_REQUEST);
        }

        // Validate exercise allows comments
        if (existingExercise.commentable == null || !existingExercise.commentable) {
            throw new WebApplicationException("Comments are not allowed on this exercise.",
                    Response.Status.BAD_REQUEST);
        }

        // Always assign the managed exercise entity
        comment.exercise = existingExercise;

        // Auto-assign current user if not provided (skip existence check)
        if (comment.user == null) {
            comment.user = this.userRepository.findByUsernameOptional(currentUsername).orElse(null);
        }

        comment.created = LocalDateTime.now();
        this.commentRepository.persist(comment);
        return new CommentViewDto(comment);
    }

    /**
     * Create a new comment with rate limiting and validation
     */
    @Transactional
    public CommentViewDto createComment(final CommentDto dto, final Long authorId) {
        LOG.info("Creating comment for exerciseId={}, authorId={}", dto.exerciseId, authorId);

        // 1. Validate input
        if (dto.content == null || dto.content.trim().isEmpty()) {
            LOG.warn("Comment creation failed: empty content for exerciseId={}, authorId={}", dto.exerciseId, authorId);
            throw new ValidationException("Content is required");
        }
        if (dto.exerciseId == null) {
            LOG.warn("Comment creation failed: missing exerciseId for authorId={}", authorId);
            throw new ValidationException("Exercise ID is required");
        }

        // 2. Check rate limiting
        try {
            this.checkRateLimit(authorId);
        } catch (final Exception e) {
            LOG.warn("Rate limit exceeded for authorId={}", authorId);
            throw e;
        }

        // 3. Validate exercise exists and allows comments
        final ExerciseEntity exercise = this.exerciseRepository.findById(dto.exerciseId);
        if (exercise == null) {
            LOG.warn("Comment creation failed: exercise not found for exerciseId={}, authorId={}", dto.exerciseId,
                    authorId);
            throw new WebApplicationException("Exercise not found", Response.Status.NOT_FOUND);
        }
        if (!Boolean.TRUE.equals(exercise.published)) {
            LOG.warn("Comment creation failed: exercise not published for exerciseId={}, authorId={}", dto.exerciseId,
                    authorId);
            throw new WebApplicationException("Cannot comment on unpublished exercise",
                    Response.Status.BAD_REQUEST);
        }
        if (!Boolean.TRUE.equals(exercise.commentable)) {
            LOG.warn("Comment creation failed: comments not allowed for exerciseId={}, authorId={}", dto.exerciseId,
                    authorId);
            throw new WebApplicationException("Comments not allowed on this exercise",
                    Response.Status.BAD_REQUEST);
        }

        // 4. Validate parent comment if threading
        CommentEntity parentComment = null;
        if (dto.parentCommentId != null) {
            LOG.debug("Creating reply comment: parentId={} for exerciseId={}, authorId={}", dto.parentCommentId,
                    dto.exerciseId, authorId);
            parentComment = this.commentRepository.findById(dto.parentCommentId);
            if (parentComment == null) {
                LOG.warn("Comment creation failed: parent comment not found for parentId={}, authorId={}",
                        dto.parentCommentId, authorId);
                throw new WebApplicationException("Parent comment not found", Response.Status.BAD_REQUEST);
            }
            if (!"VISIBLE".equals(parentComment.status)) {
                LOG.warn("Comment creation failed: cannot reply to hidden/deleted comment parentId={}, authorId={}",
                        dto.parentCommentId, authorId);
                throw new WebApplicationException("Cannot reply to deleted/hidden comment",
                        Response.Status.BAD_REQUEST);
            }
        }

        // 5. Get author
        final UserEntity author = this.userRepository.findById(authorId);
        if (author == null) {
            LOG.warn("Comment creation failed: user not found for authorId={}", authorId);
            throw new WebApplicationException("User not found", Response.Status.BAD_REQUEST);
        }

        // 6. Create and persist entity
        final CommentEntity comment = new CommentEntity();
        comment.content = dto.content.trim();
        comment.exercise = exercise;
        comment.user = author;
        comment.parentComment = parentComment;
        comment.sessionId = dto.sessionId;
        comment.created = LocalDateTime.now();
        comment.status = "VISIBLE";
        comment.flagsCount = 0;
        this.commentRepository.persist(comment);

        // 7. (relations are loaded by persist when returned) - no manual forcing
        // required

        // 8. Fire CDI event for real-time updates
        this.commentCreatedEvent.fire(new CommentCreatedEvent(
                comment.id, comment.exercise.id, comment.user.id, comment.user.username,
                comment.content, comment.created));

        LOG.info("Comment created successfully: commentId={}, exerciseId={}, authorId={}", comment.id, dto.exerciseId,
                authorId);
        return new CommentViewDto(comment);
    }

    /**
     * TODO: Document updateComment().
     */
    @Transactional
    public CommentViewDto updateComment(final CommentEntity comment) {
        final CommentEntity existingComment = this.commentRepository.findById(comment.id);
        if (existingComment == null) {
            throw new WebApplicationException("Comment not found", Response.Status.NOT_FOUND);
        }

        // Validate content is provided for complete replacement (PUT)
        if (comment.content == null || comment.content.trim().isEmpty()) {
            throw new ValidationException("Content is required for updating a comment");
        }

        // Only update content field for PUT (since we only allow content in DTO)
        existingComment.content = comment.content;

        this.commentRepository.persist(existingComment);
        return new CommentViewDto(existingComment);
    }

    /**
     * TODO: Document patchComment().
     */
    @Transactional
    public CommentViewDto patchComment(final CommentEntity comment) {
        final CommentEntity existingComment = this.commentRepository.findById(comment.id);
        if (existingComment == null) {
            throw new WebApplicationException("Comment not found", Response.Status.NOT_FOUND);
        }

        // Partial update (PATCH semantics) - only update provided fields
        if (comment.content != null) {
            existingComment.content = comment.content;
        }

        this.commentRepository.persist(existingComment);
        return new CommentViewDto(existingComment);
    }

    /**
     * TODO: Document deleteComment().
     */
    @Transactional
    public boolean deleteComment(final Long id) {
        return this.commentRepository.deleteById(id);
    }

    /**
     * Delete a comment (soft or hard)
     */
    @Transactional
    public void deleteComment(final Long commentId, final Long requesterId, final boolean softDelete) {
        LOG.info("Attempting to delete comment: commentId={}, requesterId={}, softDelete={}", commentId, requesterId,
                softDelete);

        final CommentEntity comment = this.commentRepository.findById(commentId);
        if (comment == null) {
            LOG.warn("Delete comment failed: comment not found commentId={}, requesterId={}", commentId, requesterId);
            throw new WebApplicationException("Comment not found", Response.Status.NOT_FOUND);
        }

        final UserEntity requester = this.userRepository.findById(requesterId);
        if (requester == null) {
            LOG.warn("Delete comment failed: requester not found requesterId={}", requesterId);
            throw new WebApplicationException("Requester not found", Response.Status.BAD_REQUEST);
        }

        final boolean isAuthor = comment.user.id.equals(requesterId);
        final boolean isModerator = this.isModerator(requester);

        if (!isAuthor && !isModerator) {
            LOG.warn("Delete comment unauthorized: commentId={}, requesterId={}, isAuthor={}, isModerator={}",
                    commentId, requesterId, isAuthor, isModerator);
            throw new WebApplicationException("Not authorized to delete this comment",
                    Response.Status.FORBIDDEN);
        }

        if (softDelete) {
            // Soft delete: mark as deleted but preserve data
            comment.status = "DELETED";
            comment.deletedBy = requester;
            comment.deletedAt = LocalDateTime.now();
            this.commentRepository.persist(comment);
            LOG.info("Comment soft-deleted: commentId={}, requesterId={}, isAuthor={}", commentId, requesterId,
                    isAuthor);
        } else {
            // Hard delete: remove from DB (only moderators/admins)
            if (!isModerator) {
                LOG.warn("Hard delete unauthorized: commentId={}, requesterId={}", commentId, requesterId);
                throw new WebApplicationException("Only moderators can permanently delete",
                        Response.Status.FORBIDDEN);
            }
            this.commentRepository.deleteById(commentId);
            LOG.info("Comment hard-deleted: commentId={}, requesterId={}", commentId, requesterId);
        }
    }

    /**
     * Edit a comment with permission check
     */
    @Transactional
    public CommentViewDto editComment(final Long commentId, final CommentDto dto, final Long editorId) {
        LOG.info("Attempting to edit comment: commentId={}, editorId={}", commentId, editorId);

        final CommentEntity comment = this.commentRepository.findById(commentId);
        if (comment == null) {
            LOG.warn("Edit comment failed: comment not found commentId={}, editorId={}", commentId, editorId);
            throw new WebApplicationException("Comment not found", Response.Status.NOT_FOUND);
        }

        // Check permission: only author or moderator/admin
        final UserEntity editor = this.userRepository.findById(editorId);
        if (editor == null) {
            LOG.warn("Edit comment failed: editor not found editorId={}", editorId);
            throw new WebApplicationException("Editor not found", Response.Status.BAD_REQUEST);
        }

        final boolean isAuthor = comment.user.id.equals(editorId);
        final boolean isModerator = this.isModerator(editor);

        if (!isAuthor && !isModerator) {
            LOG.warn("Edit comment unauthorized: commentId={}, editorId={}, isAuthor={}, isModerator={}", commentId,
                    editorId, isAuthor, isModerator);
            throw new WebApplicationException("Not authorized to edit this comment",
                    Response.Status.FORBIDDEN);
        }

        // Update content
        if (dto.content != null && !dto.content.trim().isEmpty()) {
            comment.content = dto.content.trim();
            comment.editedAt = LocalDateTime.now();
            this.commentRepository.persist(comment);
            LOG.info("Comment edited successfully: commentId={}, editorId={}, isAuthor={}", commentId, editorId,
                    isAuthor);
        }

        // no-op: repository methods return entities with needed relations when used
        return new CommentViewDto(comment);
    }

    /**
     * Flag a comment for moderation review
     */
    @Transactional
    public void flagComment(final Long commentId, final Long flaggerId, final String reason) {
        LOG.info("Flagging comment: commentId={}, flaggerId={}, reason={}", commentId, flaggerId, reason);

        final CommentEntity comment = this.commentRepository.findById(commentId);
        if (comment == null) {
            LOG.warn("Flag comment failed: comment not found commentId={}, flaggerId={}", commentId, flaggerId);
            throw new WebApplicationException("Comment not found", Response.Status.NOT_FOUND);
        }

        // Prevent self-flagging
        if (comment.user.id.equals(flaggerId)) {
            LOG.warn("Self-flag attempt: commentId={}, flaggerId={}", commentId, flaggerId);
            throw new WebApplicationException("Cannot flag your own comment", Response.Status.BAD_REQUEST);
        }

        // Create flag record via repository (repository handles duplicate-check)
        final var flagger = this.userRepository.findById(flaggerId);
        this.commentFlagRepository.createFlag(comment, flagger);

        // Increment flag count
        comment.flagsCount = (comment.flagsCount != null ? comment.flagsCount : 0) + 1;

        // If flagged 5+ times, auto-hide
        if (comment.flagsCount >= 5) {
            comment.status = "HIDDEN";
            LOG.warn("Comment auto-hidden due to flags: commentId={}, flagCount={}", commentId, comment.flagsCount);
        }

        this.commentRepository.persist(comment);
        LOG.info("Comment flagged: commentId={}, flaggerId={}, newFlagCount={}", commentId, flaggerId,
                comment.flagsCount);
    }

    /**
     * List comments by exercise with pagination and threading
     */
    @Transactional
    public List<CommentViewDto> listCommentsByExercise(
            final Long exerciseId,
            final int page,
            final int pageSize,
            final Long parentId) {

        List<CommentEntity> comments;
        if (parentId == null) {
            // Top-level comments (these methods already fetch relations)
            comments = this.commentRepository.findTopLevelByExercise(exerciseId, page, pageSize);
        } else {
            // Replies to specific parent
            comments = this.commentRepository.findRepliesPaged(parentId, page, pageSize);
        }

        return comments.stream()
                .map(CommentViewDto::new)
                .collect(Collectors.toList());
    }

    /**
     * List comments by session
     */
    @Transactional
    public List<CommentViewDto> listCommentsBySession(final String sessionId) {
        final List<CommentEntity> comments = this.commentRepository.findBySessionIdWithRelations(sessionId);

        return comments.stream()
                .map(CommentViewDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Moderate a comment (hide/unhide/delete)
     */
    @Transactional
    public void moderateComment(
            final Long commentId,
            final String action, // "HIDE", "SHOW", "DELETE"
            final Long moderatorId,
            final String reason) {
        LOG.info("Moderating comment: commentId={}, action={}, moderatorId={}, reason={}", commentId, action,
                moderatorId, reason);

        final CommentEntity comment = this.commentRepository.findById(commentId);
        if (comment == null) {
            LOG.warn("Moderate comment failed: comment not found commentId={}, moderatorId={}", commentId, moderatorId);
            throw new WebApplicationException("Comment not found", Response.Status.NOT_FOUND);
        }

        final UserEntity moderator = this.userRepository.findById(moderatorId);
        if (!this.isModerator(moderator)) {
            LOG.warn("Moderate comment unauthorized: commentId={}, moderatorId={}", commentId, moderatorId);
            throw new WebApplicationException("Only moderators can perform moderation",
                    Response.Status.FORBIDDEN);
        }

        switch (action.toUpperCase()) {
            case "HIDE":
                comment.status = "HIDDEN";
                LOG.info("Comment hidden by moderator: commentId={}, moderatorId={}", commentId, moderatorId);
                break;
            case "SHOW":
                comment.status = "VISIBLE";
                comment.flagsCount = 0;
                LOG.info("Comment shown by moderator: commentId={}, moderatorId={}", commentId, moderatorId);
                break;
            case "RESTORE":
                // Restore a deleted comment (same as SHOW)
                comment.status = "VISIBLE";
                comment.deletedBy = null;
                comment.deletedAt = null;
                LOG.info("Comment restored by moderator: commentId={}, moderatorId={}", commentId, moderatorId);
                break;
            case "DELETE":
                comment.status = "DELETED";
                comment.deletedBy = moderator;
                comment.deletedAt = LocalDateTime.now();
                LOG.info("Comment deleted by moderator: commentId={}, moderatorId={}", commentId, moderatorId);
                break;
            default:
                LOG.warn("Invalid moderation action: action={}, commentId={}, moderatorId={}", action, commentId,
                        moderatorId);
                throw new ValidationException("Invalid moderation action: " + action);
        }

        this.commentRepository.persist(comment);
    }

    /**
     * Find replies to a comment
     */
    @Transactional
    public List<CommentViewDto> findReplies(final Long parentCommentId) {
        final List<CommentEntity> replies = this.commentRepository.findRepliesWithRelations(parentCommentId);

        return replies.stream()
                .map(CommentViewDto::new)
                .collect(Collectors.toList());
    }

    // HELPER METHODS

    private boolean isModerator(final UserEntity user) {
        // Check if user has teacher or admin rank
        return user != null && user.rank != null && (Boolean.TRUE.equals(user.rank.exerciseEdit)
                || Boolean.TRUE.equals(user.rank.adminView));
    }

    private void checkRateLimit(final Long userId) {
        // Get user's last comment timestamp
        final LocalDateTime fiveSecondsAgo = LocalDateTime.now().minusSeconds(RATE_LIMIT_WINDOW_SECONDS);
        final long recentCount = this.commentRepository.countByUserSince(userId, fiveSecondsAgo);

        if (recentCount > 0) {
            LOG.debug("Rate limit exceeded (5-second window): userId={}, recentCount={}", userId, recentCount);
            throw new WebApplicationException("Please wait before posting another comment",
                    Response.Status.TOO_MANY_REQUESTS);
        }

        // Check daily limit
        final LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        final long dailyCount = this.commentRepository.countByUserSince(userId, oneDayAgo);

        if (dailyCount >= RATE_LIMIT_DAILY) {
            LOG.warn("Daily comment limit exceeded: userId={}, dailyCount={}, limit={}", userId, dailyCount,
                    RATE_LIMIT_DAILY);
            throw new WebApplicationException("Daily comment limit exceeded",
                    Response.Status.TOO_MANY_REQUESTS);
        }
    }

    /**
     * TODO: Document searchComments().
     */
    public List<CommentViewDto> searchComments(final String query) {
        if (query == null || query.trim().isEmpty()) {
            return this.getAllComments();
        }
        final var searchTerm = "%" + query.trim().toLowerCase() + "%";
        final List<CommentEntity> comments = this.commentRepository.search(searchTerm);
        return comments.stream()
                .map(CommentViewDto::new)
                .collect(Collectors.toList());
    }

    /**
     * TODO: Document findByDateRange().
     */
    @Transactional
    public List<CommentViewDto> findByDateRange(final String startDate, final String endDate) {
        if (startDate == null || endDate == null) {
            return this.getAllComments();
        }

        try {
            final LocalDate start = LocalDate.parse(startDate);
            final LocalDate end = LocalDate.parse(endDate);

            final LocalDateTime startDateTime = start.atStartOfDay();
            final LocalDateTime endDateTime = end.atTime(LocalTime.MAX);

            final List<CommentEntity> comments = this.commentRepository.findByDateRange(startDateTime, endDateTime);
            return comments.stream()
                    .map(CommentViewDto::new)
                    .collect(Collectors.toList());
        } catch (final Exception e) {
            // If date parsing fails, return all comments
            return this.getAllComments();
        }
    }

    /**
     * Find comments by status (VISIBLE, HIDDEN, DELETED).
     */
    @Transactional
    public List<CommentViewDto> findByStatus(final String status) {
        final List<CommentEntity> comments = this.commentRepository.findByStatus(status);
        return comments.stream()
                .map(CommentViewDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Find comments with N or more flags (for moderation).
     */
    @Transactional
    public List<CommentViewDto> findFlaggedComments(final Integer minFlags) {
        final List<CommentEntity> comments = this.commentRepository.findFlaggedComments(minFlags);
        return comments.stream()
                .map(CommentViewDto::new)
                .collect(Collectors.toList());
    }
}
