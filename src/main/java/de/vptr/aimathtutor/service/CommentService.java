package de.vptr.aimathtutor.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.vptr.aimathtutor.dto.CommentDto;
import de.vptr.aimathtutor.dto.CommentDto.CommentStatus;
import de.vptr.aimathtutor.dto.CommentViewDto;
import de.vptr.aimathtutor.entity.CommentEntity;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.event.CommentCreatedEvent;
import de.vptr.aimathtutor.repository.CommentRepository;
import de.vptr.aimathtutor.repository.ExerciseRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.comment.CommentFlaggingService;
import de.vptr.aimathtutor.service.comment.CommentModerationService;
import de.vptr.aimathtutor.service.comment.CommentPermissionService;
import de.vptr.aimathtutor.service.comment.CommentRateLimitService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Service for managing comments: creation, editing, deletion, listing and
 * moderation.
 * Delegates specialized concerns to sub-services for rate limiting,
 * flagging, moderation, and permissions.
 */
@ApplicationScoped
public class CommentService {

    private static final Logger LOG = LoggerFactory.getLogger(CommentService.class);

    // Strict policy: disallow all HTML elements/attributes. Tags are dropped and
    // residual <, >, & characters are HTML-escaped, yielding safe plain text.
    private static final PolicyFactory STRICT_HTML_POLICY = new HtmlPolicyBuilder().toFactory();

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
    CommentPermissionService commentPermissionService;

    @Inject
    CommentRateLimitService commentRateLimitService;

    @Inject
    CommentFlaggingService commentFlaggingService;

    @Inject
    CommentModerationService commentModerationService;

    /**
     * Retrieves all comments in the system with loaded relationships.
     *
     * @return a list of all {@link CommentViewDto}s with exercise, user, and parent
     *         data
     */
    @Transactional
    public List<CommentViewDto> getAllComments() {
        final List<CommentEntity> comments = this.commentRepository.findAllOrderedWithRelations();
        return comments.stream()
                .map(CommentViewDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Finds a single comment by ID with loaded relationships.
     *
     * @param id the comment ID
     * @return an {@link Optional} containing the {@link CommentViewDto} with
     *         exercise/user data, or empty if not found
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
     * Retrieves all top-level and threaded comments for a specific exercise.
     *
     * @param exerciseId the exercise ID
     * @return a list of {@link CommentViewDto}s in the exercise with loaded
     *         relationships
     */
    @Transactional
    public List<CommentViewDto> findByExerciseId(final Long exerciseId) {
        final List<CommentEntity> comments = this.commentRepository.findByExerciseIdWithRelations(exerciseId);
        return comments.stream()
                .map(CommentViewDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all comments authored by a specific user.
     *
     * @param userId the user ID
     * @return a list of {@link CommentViewDto}s authored by the user with loaded
     *         relationships
     */
    @Transactional
    public List<CommentViewDto> findByUserId(final Long userId) {
        final List<CommentEntity> comments = this.commentRepository.findByUserIdWithRelations(userId);
        return comments.stream()
                .map(CommentViewDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the most recently created comments with a limit on count.
     *
     * @param limit the maximum number of comments to return
     * @return a list of up to {@code limit} recent {@link CommentViewDto}s with
     *         loaded relationships
     */
    @Transactional
    public List<CommentViewDto> findRecentComments(final int limit) {
        final List<CommentEntity> comments = this.commentRepository.findRecentCommentsWithRelations(limit);
        return comments.stream()
                .map(CommentViewDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Creates a new comment with minimal validation (basic overload).
     * Assigns current user and sets created timestamp. Validates exercise exists,
     * is published, and allows comments.
     *
     * @param comment         the comment entity with content, exercise, and
     *                        optional parent
     * @param currentUsername the username of the current user for auto-assignment
     * @return the created {@link CommentViewDto}
     * @throws ValidationException     if content is missing or empty
     * @throws WebApplicationException if exercise not found or not commentable
     *                                 (BAD_REQUEST)
     */
    @Transactional
    public CommentViewDto createComment(final CommentEntity comment, final String currentUsername) {
        // Validate content is provided for creation
        if (comment.content == null || comment.content.isBlank()) {
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
        if (!existingExercise.published) {
            throw new WebApplicationException("Cannot add comment to an unpublished exercise.",
                    Response.Status.BAD_REQUEST);
        }

        // Validate exercise allows comments
        if (!existingExercise.commentable) {
            throw new WebApplicationException("Comments are not allowed on this exercise.",
                    Response.Status.BAD_REQUEST);
        }

        // Always assign the managed exercise entity
        comment.exercise = existingExercise;

        // Always derive the author from the trusted currentUsername; never honour
        // a caller-supplied user, which would allow impersonation.
        final UserEntity author = this.userRepository.findByUsernameOptional(currentUsername)
                .orElseThrow(() -> new WebApplicationException("Authenticated user not found",
                        Response.Status.UNAUTHORIZED));
        comment.user = author;

        // Defensively wipe the id so a caller cannot redirect the persist to an
        // existing row.
        comment.id = null;

        comment.content = this.sanitizeCommentContent(comment.content);
        this.commentRepository.persist(comment);

        // Fire CDI event for real-time updates
        if (comment.user != null) {
            this.commentCreatedEvent.fire(new CommentCreatedEvent(
                    comment.id, comment.exercise.id, comment.user.id, comment.user.username,
                    comment.content, comment.created));
        }

        return new CommentViewDto(comment);
    }

    /**
     * Create a new comment with rate limiting and validation
     */
    @Transactional
    public CommentViewDto createComment(final @Valid CommentDto dto, final Long authorId) {
        LOG.info("Creating comment for exerciseId={}, authorId={}", dto.exerciseId, authorId);

        // 1. Validate input
        if (dto.content == null || dto.content.isBlank()) {
            LOG.warn("Comment creation failed: empty content for exerciseId={}, authorId={}", dto.exerciseId, authorId);
            throw new ValidationException("Content is required");
        }
        if (dto.exerciseId == null) {
            LOG.warn("Comment creation failed: missing exerciseId for authorId={}", authorId);
            throw new ValidationException("Exercise ID is required");
        }

        // 2. Check rate limiting
        this.commentRateLimitService.checkRateLimit(authorId);

        // 3. Validate exercise exists and allows comments
        final ExerciseEntity exercise = this.exerciseRepository.findById(dto.exerciseId);
        if (exercise == null) {
            LOG.warn("Comment creation failed: exercise not found for exerciseId={}, authorId={}", dto.exerciseId,
                    authorId);
            throw new WebApplicationException("Exercise not found", Response.Status.NOT_FOUND);
        }
        if (!exercise.published) {
            LOG.warn("Comment creation failed: exercise not published for exerciseId={}, authorId={}", dto.exerciseId,
                    authorId);
            throw new WebApplicationException("Cannot comment on unpublished exercise",
                    Response.Status.BAD_REQUEST);
        }
        if (!exercise.commentable) {
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
            if (parentComment.status != CommentStatus.VISIBLE) {
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
        comment.content = this.sanitizeCommentContent(dto.content);
        comment.exercise = exercise;
        comment.user = author;
        comment.parentComment = parentComment;
        comment.sessionId = dto.sessionId;
        comment.status = CommentStatus.VISIBLE;
        comment.flagsCount = 0;
        this.commentRepository.persist(comment);

        // 7. Fire CDI event for real-time updates
        this.commentCreatedEvent.fire(new CommentCreatedEvent(
                comment.id, comment.exercise.id, comment.user.id, comment.user.username,
                comment.content, comment.created));

        LOG.info("Comment created successfully: commentId={}, exerciseId={}, authorId={}", comment.id, dto.exerciseId,
                authorId);
        return new CommentViewDto(comment);
    }

    /**
     * Sanitizes comment content by stripping HTML tags to prevent stored XSS.
     *
     * @param content raw comment content
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
     * Completely replaces an existing comment (PUT semantics).
     * Only content field is updated; exercise, user, and parent remain unchanged.
     * Package-private to enforce permission checks through the public API.
     *
     * @param comment the comment entity with id and updated content
     * @return the updated {@link CommentViewDto}
     * @throws WebApplicationException if comment not found (NOT_FOUND status)
     * @throws ValidationException     if content is missing or empty
     */
    @Transactional
    CommentViewDto updateComment(final CommentEntity comment) {
        final CommentEntity existingComment = this.commentRepository.findById(comment.id);
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
     * Partially updates an existing comment (PATCH semantics).
     * Only updates comment properties that are explicitly provided in the entity;
     * null values are ignored.
     * Package-private to enforce permission checks through the public API.
     *
     * @param comment the comment entity with id and partial fields to update
     * @return the updated {@link CommentViewDto}
     * @throws WebApplicationException if comment not found (NOT_FOUND status)
     */
    @Transactional
    CommentViewDto patchComment(final CommentEntity comment) {
        final CommentEntity existingComment = this.commentRepository.findById(comment.id);
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
     * Deletes a comment by ID (basic overload).
     * Package-private to enforce permission checks through the public API.
     *
     * @param id the comment ID to delete
     * @return {@code true} if deletion succeeded, {@code false} if comment not
     *         found
     */
    @Transactional
    boolean deleteComment(final Long id) {
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

        this.commentPermissionService.verifyCanDelete(comment, requester, softDelete);

        final boolean isAuthor = comment.user != null && comment.user.id.equals(requesterId);

        if (softDelete) {
            // Soft delete: mark as deleted but preserve data
            comment.status = CommentStatus.DELETED;
            comment.deletedBy = requester;
            comment.deletedAt = LocalDateTime.now();
            this.commentRepository.persist(comment);
            LOG.info("Comment soft-deleted: commentId={}, requesterId={}, isAuthor={}", commentId, requesterId,
                    isAuthor);
        } else {
            this.commentRepository.deleteById(commentId);
            LOG.info("Comment hard-deleted: commentId={}, requesterId={}", commentId, requesterId);
        }
    }

    /**
     * Edit a comment with permission check
     */
    @Transactional
    public CommentViewDto editComment(final Long commentId, final @Valid CommentDto dto, final Long editorId) {
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

        this.commentPermissionService.verifyCanEdit(comment, editor);

        // Update content
        if (dto.content != null && !dto.content.isBlank()) {
            comment.content = this.sanitizeCommentContent(dto.content);
            this.commentRepository.persist(comment);
            LOG.info("Comment edited successfully: commentId={}, editorId={}, isAuthor={}", commentId, editorId,
                    comment.user != null && comment.user.id.equals(editorId));
        }

        return new CommentViewDto(comment);
    }

    /**
     * Flag a comment for moderation review
     */
    @Transactional
    public void flagComment(final Long commentId, final Long flaggerId, final String reason) {
        this.commentFlaggingService.flagComment(commentId, flaggerId, reason);
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
            final String action,
            final Long moderatorId,
            final String reason) {
        this.commentModerationService.moderateComment(commentId, action, moderatorId, reason);
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

    /**
     * Searches comments by content using the provided query string
     * (case-insensitive).
     * Returns an empty list if query is null or empty to avoid loading full
     * datasets.
     *
     * @param query the search query string (content match)
     * @return a list of matching {@link CommentViewDto}s
     */
    public List<CommentViewDto> searchComments(final String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        final var searchTerm = "%" + query.trim().toLowerCase() + "%";
        final List<CommentEntity> comments = this.commentRepository.search(searchTerm);
        return comments.stream()
                .map(CommentViewDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Finds comments created within a date range (inclusive).
     * Date strings are parsed as ISO-8601 dates. Returns an empty list if parsing
     * fails or dates are null.
     *
     * @param startDate the start date (ISO-8601 format: YYYY-MM-DD)
     * @param endDate   the end date (ISO-8601 format: YYYY-MM-DD)
     * @return a list of {@link CommentViewDto}s created within the date range
     */
    @Transactional
    public List<CommentViewDto> findByDateRange(final String startDate, final String endDate) {
        if (startDate == null || endDate == null) {
            return List.of();
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
        } catch (final DateTimeParseException e) {
            LOG.warn("Invalid date range provided: startDate='{}', endDate='{}'", startDate, endDate);
            return List.of();
        }
    }

    /**
     * Find comments by status (VISIBLE, HIDDEN, DELETED).
     */
    @Transactional
    public List<CommentViewDto> findByStatus(final CommentStatus status) {
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
        return this.commentFlaggingService.findFlaggedComments(minFlags);
    }
}
