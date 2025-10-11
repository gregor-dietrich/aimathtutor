package de.vptr.aimathtutor.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import de.vptr.aimathtutor.dto.CommentViewDto;
import de.vptr.aimathtutor.entity.CommentEntity;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class CommentService {

    @Inject
    UserService userService;

    @Transactional
    public List<CommentViewDto> getAllComments() {
        final List<CommentEntity> comments = CommentEntity.listAll();
        // Force load lazy fields within transaction
        for (final CommentEntity comment : comments) {
            comment.exercise.title.length(); // Force load exercise title
            comment.user.username.length(); // Force load username
            comment.content.length(); // Force load content
        }
        return comments.stream()
                .map(CommentViewDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public Optional<CommentViewDto> findById(final Long id) {
        final Optional<CommentEntity> comment = CommentEntity.findByIdOptional(id);
        if (comment.isPresent()) {
            final CommentEntity entity = comment.get();
            // Force load lazy fields within transaction
            entity.exercise.title.length(); // Force load exercise title
            entity.user.username.length(); // Force load username
            entity.content.length(); // Force load content
            return Optional.of(new CommentViewDto(entity));
        }
        return Optional.empty();
    }

    @Transactional
    public List<CommentViewDto> findByExerciseId(final Long exerciseId) {
        final List<CommentEntity> comments = CommentEntity.find("post.id", exerciseId).list();
        // Force load lazy fields within transaction
        for (final CommentEntity comment : comments) {
            comment.exercise.title.length(); // Force load exercise title
            comment.user.username.length(); // Force load username
            comment.content.length(); // Force load content
        }
        return comments.stream()
                .map(CommentViewDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<CommentViewDto> findByUserId(final Long userId) {
        final List<CommentEntity> comments = CommentEntity.find("user.id", userId).list();
        // Force load lazy fields within transaction
        for (final CommentEntity comment : comments) {
            comment.exercise.title.length(); // Force load exercise title
            comment.user.username.length(); // Force load username
            comment.content.length(); // Force load content
        }
        return comments.stream()
                .map(CommentViewDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<CommentViewDto> findRecentComments(final int limit) {
        final List<CommentEntity> comments = CommentEntity.findRecentComments(limit);
        // Force initialization of lazy fields
        for (final CommentEntity comment : comments) {
            if (comment.exercise != null) {
                comment.exercise.title.length(); // Force lazy loading
            }
            if (comment.user != null) {
                comment.user.username.length(); // Force lazy loading
            }
            comment.content.length(); // Force load content
        }
        return comments.stream()
                .map(CommentViewDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentViewDto createComment(final CommentEntity comment, final String currentUsername) {
        // Validate content is provided for creation
        if (comment.content == null || comment.content.trim().isEmpty()) {
            throw new ValidationException("Content is required for creating a comment");
        }

        // Validate exercise exists
        final ExerciseEntity existingExercise = ExerciseEntity
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
            comment.user = (UserEntity) UserEntity.find("username", currentUsername).firstResultOptional().orElse(null);
        }

        comment.created = LocalDateTime.now();
        comment.persist();

        // Force load lazy fields to avoid LazyInitializationException
        if (comment.exercise != null) {
            comment.exercise.title.length(); // Force lazy loading
        }
        if (comment.user != null) {
            comment.user.username.length(); // Force lazy loading
        }

        return new CommentViewDto(comment);
    }

    @Transactional
    public CommentViewDto updateComment(final CommentEntity comment) {
        final CommentEntity existingComment = CommentEntity.findById(comment.id);
        if (existingComment == null) {
            throw new WebApplicationException("Comment not found", Response.Status.NOT_FOUND);
        }

        // Validate content is provided for complete replacement (PUT)
        if (comment.content == null || comment.content.trim().isEmpty()) {
            throw new ValidationException("Content is required for updating a comment");
        }

        // Only update content field for PUT (since we only allow content in DTO)
        existingComment.content = comment.content;

        existingComment.persist();

        // Force initialization of lazy fields to avoid LazyInitializationException
        if (existingComment.exercise != null) {
            existingComment.exercise.title.length(); // Force lazy loading
        }
        if (existingComment.user != null) {
            existingComment.user.username.length(); // Force lazy loading
        }

        return new CommentViewDto(existingComment);
    }

    @Transactional
    public CommentViewDto patchComment(final CommentEntity comment) {
        final CommentEntity existingComment = CommentEntity.findById(comment.id);
        if (existingComment == null) {
            throw new WebApplicationException("Comment not found", Response.Status.NOT_FOUND);
        }

        // Partial update (PATCH semantics) - only update provided fields
        if (comment.content != null) {
            existingComment.content = comment.content;
        }

        existingComment.persist();

        // Force initialization of lazy fields to avoid LazyInitializationException
        if (existingComment.exercise != null) {
            existingComment.exercise.title.length(); // Force lazy loading
        }
        if (existingComment.user != null) {
            existingComment.user.username.length(); // Force lazy loading
        }

        return new CommentViewDto(existingComment);
    }

    @Transactional
    public boolean deleteComment(final Long id) {
        return CommentEntity.deleteById(id);
    }

    public List<CommentViewDto> searchComments(final String query) {
        if (query == null || query.trim().isEmpty()) {
            return this.getAllComments();
        }
        final var searchTerm = "%" + query.trim().toLowerCase() + "%";
        final List<CommentEntity> comments = CommentEntity.find(
                "content LIKE ?1 OR LOWER(user.username) LIKE ?1", searchTerm).list();

        // Force load lazy fields
        for (final CommentEntity comment : comments) {
            if (comment.exercise != null) {
                comment.exercise.title.length(); // Force lazy loading
            }
            if (comment.user != null) {
                comment.user.username.length(); // Force lazy loading
            }
            comment.content.length(); // Force load content
        }

        return comments.stream()
                .map(CommentViewDto::new)
                .collect(Collectors.toList());
    }

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

            final List<CommentEntity> comments = CommentEntity
                    .find("created >= ?1 AND created <= ?2", startDateTime, endDateTime).list();

            // Force load lazy fields
            for (final CommentEntity comment : comments) {
                if (comment.exercise != null) {
                    comment.exercise.title.length(); // Force lazy loading
                }
                if (comment.user != null) {
                    comment.user.username.length(); // Force lazy loading
                }
                comment.content.length(); // Force load content
            }

            return comments.stream()
                    .map(CommentViewDto::new)
                    .collect(Collectors.toList());
        } catch (final Exception e) {
            // If date parsing fails, return all comments
            return this.getAllComments();
        }
    }
}
