package de.vptr.aimathtutor.service.comment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import de.vptr.aimathtutor.dto.CommentDto;
import de.vptr.aimathtutor.dto.CommentDto.CommentStatus;
import de.vptr.aimathtutor.dto.CommentViewDto;
import de.vptr.aimathtutor.dto.SessionCredentials;
import de.vptr.aimathtutor.entity.CommentEntity;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.event.CommentCreatedEvent;
import de.vptr.aimathtutor.exception.PermissionDeniedException;
import de.vptr.aimathtutor.repository.CommentRepository;
import de.vptr.aimathtutor.repository.ExerciseRepository;
import de.vptr.aimathtutor.service.security.AuthService;
import de.vptr.aimathtutor.service.security.PermissionService;
import de.vptr.aimathtutor.util.SearchPatternUtil;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Service for managing comments: creation, editing, deletion, listing and moderation. Delegates specialized concerns to
 * sub-services for rate limiting, flagging, moderation, and permissions.
 */
@ApplicationScoped
public class CommentService {

    private static final Logger LOG = Logger.getLogger(CommentService.class);

    // Strict policy: disallow all HTML elements/attributes. Tags are dropped and
    // residual <, >, & characters are HTML-escaped, yielding safe plain text.
    private static final PolicyFactory STRICT_HTML_POLICY = new HtmlPolicyBuilder().toFactory();

    @Inject
    Event<CommentCreatedEvent> commentCreatedEvent;

    @Inject
    CommentRepository commentRepository;

    @Inject
    ExerciseRepository exerciseRepository;

    @Inject
    CommentPermissionService commentPermissionService;

    @Inject
    CommentRateLimitService commentRateLimitService;

    @Inject
    CommentFlaggingService commentFlaggingService;

    @Inject
    CommentModerationService commentModerationService;

    @Inject
    PermissionService permissionService;

    @Inject
    AuthService authService;

    /**
     * Retrieves all comments in the system with loaded relationships.
     *
     * @param credentials
     *            the credentials of the calling session, captured on the UI thread
     * @return a list of all {@link CommentViewDto}s with exercise, user, and parent data
     * @throws ValidationException
     *             if the session has ended
     * @throws PermissionDeniedException
     *             if the caller may not view the administration
     */
    @Transactional
    public List<CommentViewDto> getAllComments(@Nullable final SessionCredentials credentials) {
        return this.getAllComments(credentials, 0, 1000);
    }

    /**
     * Retrieves comments with pagination.
     *
     * @param credentials
     *            the credentials of the calling session, captured on the UI thread
     * @param page
     *            the page number (0-indexed)
     * @param pageSize
     *            the number of comments per page
     * @return a paginated list of {@link CommentViewDto}s
     * @throws ValidationException
     *             if the session has ended
     * @throws PermissionDeniedException
     *             if the caller may not view the administration
     */
    @Transactional
    public List<CommentViewDto> getAllComments(@Nullable final SessionCredentials credentials, final int page,
            final int pageSize) {
        this.permissionService.requireAdminView(credentials);
        final List<CommentEntity> comments = this.commentRepository.findAllOrderedWithRelations(page, pageSize);
        return comments.stream().map(CommentViewDto::new).collect(Collectors.toList());
    }

    /**
     * Retrieves all top-level and threaded comments for a specific exercise.
     *
     * @param credentials
     *            the credentials of the calling session, captured on the UI thread
     * @param exerciseId
     *            the exercise ID
     * @return a list of {@link CommentViewDto}s in the exercise with loaded relationships
     * @throws ValidationException
     *             if the session has ended
     * @throws PermissionDeniedException
     *             if the caller may not view the administration
     */
    @Transactional
    public List<CommentViewDto> findByExerciseId(@Nullable final SessionCredentials credentials,
            final Long exerciseId) {
        return this.findByExerciseId(credentials, exerciseId, 0, 1000);
    }

    /**
     * Retrieves comments for a specific exercise with pagination.
     *
     * @param credentials
     *            the credentials of the calling session, captured on the UI thread
     * @param exerciseId
     *            the exercise ID
     * @param page
     *            the page number (0-indexed)
     * @param pageSize
     *            the number of comments per page
     * @return a paginated list of {@link CommentViewDto}s
     * @throws ValidationException
     *             if the session has ended
     * @throws PermissionDeniedException
     *             if the caller may not view the administration
     */
    @Transactional
    public List<CommentViewDto> findByExerciseId(@Nullable final SessionCredentials credentials, final Long exerciseId,
            final int page, final int pageSize) {
        this.permissionService.requireAdminView(credentials);
        final List<CommentEntity> comments =
                this.commentRepository.findByExerciseIdWithRelations(exerciseId, page, pageSize);
        return comments.stream().map(CommentViewDto::new).collect(Collectors.toList());
    }

    /**
     * Retrieves all comments authored by a specific user.
     *
     * @param credentials
     *            the credentials of the calling session, captured on the UI thread
     * @param userId
     *            the user ID
     * @return a list of {@link CommentViewDto}s authored by the user with loaded relationships
     * @throws ValidationException
     *             if the session has ended
     * @throws PermissionDeniedException
     *             if the caller may not view the administration
     */
    @Transactional
    public List<CommentViewDto> findByUserId(@Nullable final SessionCredentials credentials, final Long userId) {
        this.permissionService.requireAdminView(credentials);
        final List<CommentEntity> comments = this.commentRepository.findByUserIdWithRelations(userId);
        return comments.stream().map(CommentViewDto::new).collect(Collectors.toList());
    }

    /**
     * Create a new comment with rate limiting and validation, authored by the signed-in user. Requires the
     * {@code commentAdd} permission.
     *
     * @throws ValidationException
     *             if the session has ended or the input is invalid
     * @throws PermissionDeniedException
     *             if the caller may not add comments
     */
    @Transactional
    public CommentViewDto createComment(final @Valid CommentDto dto) {
        final UserEntity author = this.authService.requireCurrentUser();
        final Long authorId = Objects.requireNonNull(author.id);
        LOG.infof("Creating comment for exercisePublicId=%s, authorId=%s", dto.exercisePublicId, authorId);

        this.permissionService.requireCommentAdd();

        // 1. Validate input
        if (dto.content == null || dto.content.isBlank()) {
            LOG.warnf("Comment creation failed: empty content for exercisePublicId=%s, authorId=%s",
                    dto.exercisePublicId, authorId);
            throw new ValidationException("Content is required");
        }
        if (dto.exercisePublicId == null) {
            LOG.warnf("Comment creation failed: missing exercisePublicId for authorId=%s", authorId);
            throw new ValidationException("Exercise ID is required");
        }

        // 2. Check rate limiting
        this.commentRateLimitService.checkRateLimit(authorId);

        // 3. Validate exercise exists and allows comments
        final ExerciseEntity exercise = this.exerciseRepository.findByPublicId(dto.exercisePublicId).orElse(null);
        if (exercise == null) {
            LOG.warnf("Comment creation failed: exercise not found for exercisePublicId=%s, authorId=%s",
                    dto.exercisePublicId, authorId);
            throw new WebApplicationException("Exercise not found", Response.Status.NOT_FOUND);
        }
        if (!exercise.published) {
            LOG.warnf("Comment creation failed: exercise not published for exercisePublicId=%s, authorId=%s",
                    dto.exercisePublicId, authorId);
            throw new WebApplicationException("Cannot comment on unpublished exercise", Response.Status.BAD_REQUEST);
        }
        if (!exercise.commentable) {
            LOG.warnf("Comment creation failed: comments not allowed for exercisePublicId=%s, authorId=%s",
                    dto.exercisePublicId, authorId);
            throw new WebApplicationException("Comments not allowed on this exercise", Response.Status.BAD_REQUEST);
        }

        // 4. Validate parent comment if threading
        CommentEntity parentComment = null;
        if (dto.parentCommentPublicId != null) {
            LOG.debugf("Creating reply comment: parentPublicId=%s for exercisePublicId=%s, authorId=%s",
                    dto.parentCommentPublicId, dto.exercisePublicId, authorId);
            parentComment = this.commentRepository.findByPublicId(dto.parentCommentPublicId).orElse(null);
            if (parentComment == null) {
                LOG.warnf("Comment creation failed: parent comment not found for parentPublicId=%s, authorId=%s",
                        dto.parentCommentPublicId, authorId);
                throw new WebApplicationException("Parent comment not found", Response.Status.BAD_REQUEST);
            }
            if (parentComment.status != CommentStatus.VISIBLE) {
                LOG.warnf("Comment creation failed: cannot reply to hidden/deleted comment "
                        + "parentPublicId=%s, authorId=%s", dto.parentCommentPublicId, authorId);
                throw new WebApplicationException("Cannot reply to deleted/hidden comment",
                        Response.Status.BAD_REQUEST);
            }
            if (parentComment.exercise == null
                    || !Objects.equals(parentComment.exercise.publicId, dto.exercisePublicId)) {
                LOG.warnf(
                        "Comment creation failed: parent comment belongs to different exercise. "
                                + "parentPublicId=%s, parentExercisePublicId=%s, dto.exercisePublicId=%s, authorId=%s",
                        dto.parentCommentPublicId,
                        parentComment.exercise != null ? parentComment.exercise.publicId : null, dto.exercisePublicId,
                        authorId);
                throw new WebApplicationException("Parent comment belongs to a different exercise",
                        Response.Status.BAD_REQUEST);
            }
        }

        // 5. Create and persist entity
        final CommentEntity comment = new CommentEntity();
        comment.content = this.sanitizeCommentContent(dto.content);
        comment.exercise = exercise;
        comment.user = author;
        comment.parentComment = parentComment;
        comment.sessionId = dto.sessionId;
        comment.status = CommentStatus.VISIBLE;
        comment.flagsCount = 0;
        this.commentRepository.persist(comment);

        // 6. Fire CDI event for real-time updates
        final Long commentId = comment.id;
        final Long exerciseId = comment.exercise != null ? comment.exercise.id : null;
        final Long userId = comment.user != null ? comment.user.id : null;
        final String username = comment.user != null ? comment.user.username : null;
        final String content = comment.content;
        final LocalDateTime created = comment.created;

        if (commentId != null && exerciseId != null && userId != null && username != null && content != null
                && created != null) {
            this.commentCreatedEvent
                    .fire(new CommentCreatedEvent(commentId, exerciseId, userId, username, content, created));
        } else {
            LOG.warnf("Comment created but missing mandatory fields for event: id=%s, exerciseId=%s, userId=%s",
                    commentId, exerciseId, userId);
        }

        LOG.infof("Comment created successfully: commentId=%s, exercisePublicId=%s, authorId=%s", comment.id,
                dto.exercisePublicId, authorId);
        return new CommentViewDto(comment);
    }

    /**
     * Sanitizes comment content by stripping HTML tags to prevent stored XSS.
     *
     * @param content
     *            raw comment content
     * @return sanitized content with HTML tags removed
     */
    private String sanitizeCommentContent(final String content) {
        if (content == null) {
            throw new ValidationException("Comment content cannot be null");
        }
        final String sanitized = STRICT_HTML_POLICY.sanitize(content).trim();
        if (sanitized.isEmpty()) {
            throw new ValidationException("Comment content cannot be blank after sanitization");
        }
        return sanitized;
    }

    /**
     * Completely replaces an existing comment (PUT semantics). Only content field is updated; exercise, user, and
     * parent remain unchanged. Package-private to enforce permission checks through the public API.
     *
     * @param comment
     *            the comment entity with id and updated content
     * @return the updated {@link CommentViewDto}
     * @throws WebApplicationException
     *             if comment not found (NOT_FOUND status)
     * @throws ValidationException
     *             if content is missing or empty
     */
    @Transactional
    CommentViewDto updateComment(final CommentEntity comment) {
        final CommentEntity existingComment = this.commentRepository.findById(Objects.requireNonNull(comment.id));
        if (existingComment == null) {
            throw new WebApplicationException("Comment not found", Response.Status.NOT_FOUND);
        }

        // Validate content is provided for complete replacement (PUT)
        if (comment.content == null || comment.content.isBlank()) {
            throw new ValidationException("Content is required for updating a comment");
        }

        // Only update content field for PUT (since we only allow content in DTO)
        existingComment.content = this.sanitizeCommentContent(comment.content);

        this.commentRepository.persist(existingComment);
        return new CommentViewDto(existingComment);
    }

    /**
     * Partially updates an existing comment (PATCH semantics). Only updates comment properties that are explicitly
     * provided in the entity; null values are ignored. Package-private to enforce permission checks through the public
     * API.
     *
     * @param comment
     *            the comment entity with id and partial fields to update
     * @return the updated {@link CommentViewDto}
     * @throws WebApplicationException
     *             if comment not found (NOT_FOUND status)
     */
    @Transactional
    CommentViewDto patchComment(final CommentEntity comment) {
        final CommentEntity existingComment = this.commentRepository.findById(Objects.requireNonNull(comment.id));
        if (existingComment == null) {
            throw new WebApplicationException("Comment not found", Response.Status.NOT_FOUND);
        }

        // Partial update (PATCH semantics) - only update provided fields
        if (comment.content != null) {
            existingComment.content = this.sanitizeCommentContent(comment.content);
        }

        this.commentRepository.persist(existingComment);
        return new CommentViewDto(existingComment);
    }

    /**
     * Deletes a comment by ID (basic overload). Package-private to enforce permission checks through the public API.
     *
     * @param id
     *            the comment ID to delete
     * @return {@code true} if deletion succeeded, {@code false} if comment not found
     */
    @Transactional
    boolean deleteComment(final Long id) {
        return this.commentRepository.deleteById(id);
    }

    /**
     * Delete a comment (soft or hard) as the signed-in user.
     *
     * @throws ValidationException
     *             if the session has ended
     * @throws PermissionDeniedException
     *             if the caller deletes another user's comment, or hard-deletes one, without permission to delete
     *             comments
     */
    @Transactional
    public void deleteComment(final String commentPublicId, final boolean softDelete) {
        final UserEntity requester = this.authService.requireCurrentUser();
        final Long requesterId = requester.id;
        LOG.infof("Attempting to delete comment: commentPublicId=%s, requesterId=%s, softDelete=%s", commentPublicId,
                requesterId, softDelete);

        final CommentEntity comment = this.commentRepository.findByPublicId(commentPublicId).orElse(null);
        if (comment == null) {
            LOG.warnf("Delete comment failed: comment not found commentPublicId=%s, requesterId=%s", commentPublicId,
                    requesterId);
            throw new WebApplicationException("Comment not found", Response.Status.NOT_FOUND);
        }

        final boolean isAuthor = this.commentPermissionService.isAuthor(comment, requester);

        if (softDelete) {
            if (!isAuthor) {
                this.permissionService.requireCommentDelete();
            }
            comment.status = CommentStatus.DELETED;
            comment.deletedAt = LocalDateTime.now(ZoneId.systemDefault());
            comment.deletedBy = requester;
            LOG.infof("Comment soft-deleted: commentPublicId=%s, requesterId=%s", commentPublicId, requesterId);
        } else {
            this.permissionService.requireCommentDelete();
            this.commentRepository.deleteByPublicId(commentPublicId);
            LOG.infof("Comment hard-deleted: commentPublicId=%s, requesterId=%s", commentPublicId, requesterId);
        }
    }

    /**
     * Edit a comment as the signed-in user, with permission check
     *
     * @throws ValidationException
     *             if the session has ended or the input is invalid
     * @throws PermissionDeniedException
     *             if the caller may not edit other users' comments
     */
    @Transactional
    public CommentViewDto editComment(final String commentPublicId, final @Valid CommentDto dto) {
        final UserEntity editor = this.authService.requireCurrentUser();
        final Long editorId = editor.id;
        LOG.infof("Attempting to edit comment: commentPublicId=%s, editorId=%s", commentPublicId, editorId);

        final CommentEntity comment = this.commentRepository.findByPublicId(commentPublicId).orElse(null);
        if (comment == null) {
            LOG.warnf("Edit comment failed: comment not found commentPublicId=%s, editorId=%s", commentPublicId,
                    editorId);
            throw new WebApplicationException("Comment not found", Response.Status.NOT_FOUND);
        }

        // Authors can always edit their own comments; non-authors need explicit
        // permission
        if (!this.commentPermissionService.isAuthor(comment, editor)) {
            this.permissionService.requireCommentEdit();
        }
        // Update content
        if (dto.content != null && !dto.content.isBlank()) {
            comment.content = this.sanitizeCommentContent(dto.content);
            this.commentRepository.persist(comment);
            LOG.infof("Comment edited successfully: commentPublicId=%s, editorId=%s, isAuthor=%s", commentPublicId,
                    editorId, Objects.equals(comment.user != null ? comment.user.publicId : null, editor.publicId));
        }

        return new CommentViewDto(comment);
    }

    /**
     * Flag a comment for moderation review as the signed-in user
     *
     * @throws ValidationException
     *             if the session has ended
     */
    @Transactional
    public void flagComment(final String commentPublicId, final String reason) {
        this.commentFlaggingService.flagComment(commentPublicId, this.authService.requireCurrentUser(), reason);
    }

    /**
     * List comments by exercise with pagination and threading
     */
    @Transactional
    public List<CommentViewDto> listCommentsByExercise(final Long exerciseId, final int page, final int pageSize,
            @Nullable final String parentPublicId) {

        List<CommentEntity> comments;
        if (parentPublicId == null) {
            // Top-level comments (these methods already fetch relations)
            comments = this.commentRepository.findTopLevelByExercise(exerciseId, page, pageSize);
        } else {
            // Replies to specific parent
            comments = this.commentRepository.findRepliesPaged(parentPublicId, page, pageSize);
        }

        return comments.stream().map(CommentViewDto::new).collect(Collectors.toList());
    }

    /**
     * Moderate a comment (hide/unhide/delete) as the signed-in user. Requires the {@code commentEdit} permission.
     *
     * @throws ValidationException
     *             if the session has ended or the input is invalid
     * @throws PermissionDeniedException
     *             if the caller may not edit comments
     */
    @Transactional
    public void moderateComment(final String commentPublicId, final String action, final String reason) {
        final UserEntity moderator = this.authService.requireCurrentUser();
        this.permissionService.requireCommentEdit();
        this.commentModerationService.moderateComment(commentPublicId, action, moderator, reason);
    }

    /**
     * Find replies to a comment
     */
    @Transactional
    public List<CommentViewDto> findReplies(final String parentPublicId) {
        final List<CommentEntity> replies = this.commentRepository.findRepliesWithRelations(parentPublicId);

        return replies.stream().map(CommentViewDto::new).collect(Collectors.toList());
    }

    /**
     * Searches comments by content using the provided query string (case-insensitive). Returns an empty list if query
     * is null or empty to avoid loading full datasets.
     *
     * @param credentials
     *            the credentials of the calling session, captured on the UI thread
     * @param query
     *            the search query string (content match)
     * @return a list of matching {@link CommentViewDto}s
     * @throws ValidationException
     *             if the session has ended
     * @throws PermissionDeniedException
     *             if the caller may not view the administration
     */
    @Transactional
    public List<CommentViewDto> searchComments(@Nullable final SessionCredentials credentials, final String query) {
        this.permissionService.requireAdminView(credentials);
        if (query == null || query.isBlank()) {
            return List.of();
        }
        final var searchTerm = SearchPatternUtil.containsPattern(query.trim().toLowerCase(Locale.ROOT));
        final List<CommentEntity> comments = this.commentRepository.search(searchTerm);
        return comments.stream().map(CommentViewDto::new).collect(Collectors.toList());
    }

    /**
     * Finds comments created within a date range (inclusive). Date strings are parsed as ISO-8601 dates. Returns an
     * empty list if parsing fails or dates are null.
     *
     * @param credentials
     *            the credentials of the calling session, captured on the UI thread
     * @param startDate
     *            the start date (ISO-8601 format: YYYY-MM-DD)
     * @param endDate
     *            the end date (ISO-8601 format: YYYY-MM-DD)
     * @return a list of {@link CommentViewDto}s created within the date range
     * @throws ValidationException
     *             if the session has ended
     * @throws PermissionDeniedException
     *             if the caller may not view the administration
     */
    @Transactional
    public List<CommentViewDto> findByDateRange(@Nullable final SessionCredentials credentials, final String startDate,
            final String endDate) {
        this.permissionService.requireAdminView(credentials);
        if (startDate == null || endDate == null) {
            return List.of();
        }

        try {
            final LocalDate start = LocalDate.parse(startDate);
            final LocalDate end = LocalDate.parse(endDate);

            final LocalDateTime startDateTime = start.atStartOfDay();
            final LocalDateTime endDateTime = end.atTime(LocalTime.MAX);

            final List<CommentEntity> comments = this.commentRepository.findByDateRange(startDateTime, endDateTime);
            return comments.stream().map(CommentViewDto::new).collect(Collectors.toList());
        } catch (final DateTimeParseException e) {
            LOG.warnf(e, "Invalid date range provided: startDate='%s', endDate='%s'", startDate, endDate);
            return List.of();
        }
    }

    /**
     * Find comments by status (VISIBLE, HIDDEN, DELETED).
     *
     * @param credentials
     *            the credentials of the calling session, captured on the UI thread
     * @throws ValidationException
     *             if the session has ended
     * @throws PermissionDeniedException
     *             if the caller may not view the administration
     */
    @Transactional
    public List<CommentViewDto> findByStatus(@Nullable final SessionCredentials credentials,
            final CommentStatus status) {
        this.permissionService.requireAdminView(credentials);
        final List<CommentEntity> comments = this.commentRepository.findByStatus(status);
        return comments.stream().map(CommentViewDto::new).collect(Collectors.toList());
    }

    /**
     * Find comments with N or more flags (for moderation).
     *
     * @param credentials
     *            the credentials of the calling session, captured on the UI thread
     * @throws ValidationException
     *             if the session has ended
     * @throws PermissionDeniedException
     *             if the caller may not view the administration
     */
    @Transactional
    public List<CommentViewDto> findFlaggedComments(@Nullable final SessionCredentials credentials,
            final Integer minFlags) {
        this.permissionService.requireAdminView(credentials);
        return this.commentFlaggingService.findFlaggedComments(minFlags);
    }
}
