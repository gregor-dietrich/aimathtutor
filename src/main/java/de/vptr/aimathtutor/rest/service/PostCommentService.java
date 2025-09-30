package de.vptr.aimathtutor.rest.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import de.vptr.aimathtutor.rest.dto.PostCommentViewDto;
import de.vptr.aimathtutor.rest.entity.PostCommentEntity;
import de.vptr.aimathtutor.rest.entity.PostEntity;
import de.vptr.aimathtutor.rest.entity.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class PostCommentService {

    @Inject
    UserService userService;

    @Transactional
    public List<PostCommentViewDto> getAllComments() {
        final List<PostCommentEntity> comments = PostCommentEntity.listAll();
        // Force load lazy fields within transaction
        for (final PostCommentEntity comment : comments) {
            comment.post.title.length(); // Force load post title
            comment.user.username.length(); // Force load username
            comment.content.length(); // Force load content
        }
        return comments.stream()
                .map(PostCommentViewDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public Optional<PostCommentViewDto> findById(final Long id) {
        final Optional<PostCommentEntity> comment = PostCommentEntity.findByIdOptional(id);
        if (comment.isPresent()) {
            final PostCommentEntity entity = comment.get();
            // Force load lazy fields within transaction
            entity.post.title.length(); // Force load post title
            entity.user.username.length(); // Force load username
            entity.content.length(); // Force load content
            return Optional.of(new PostCommentViewDto(entity));
        }
        return Optional.empty();
    }

    @Transactional
    public List<PostCommentViewDto> findByPostId(final Long postId) {
        final List<PostCommentEntity> comments = PostCommentEntity.find("post.id", postId).list();
        // Force load lazy fields within transaction
        for (final PostCommentEntity comment : comments) {
            comment.post.title.length(); // Force load post title
            comment.user.username.length(); // Force load username
            comment.content.length(); // Force load content
        }
        return comments.stream()
                .map(PostCommentViewDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<PostCommentViewDto> findByUserId(final Long userId) {
        final List<PostCommentEntity> comments = PostCommentEntity.find("user.id", userId).list();
        // Force load lazy fields within transaction
        for (final PostCommentEntity comment : comments) {
            comment.post.title.length(); // Force load post title
            comment.user.username.length(); // Force load username
            comment.content.length(); // Force load content
        }
        return comments.stream()
                .map(PostCommentViewDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<PostCommentViewDto> findRecentComments(final int limit) {
        final List<PostCommentEntity> comments = PostCommentEntity.findRecentComments(limit);
        // Force initialization of lazy fields
        for (final PostCommentEntity comment : comments) {
            if (comment.post != null) {
                comment.post.title.length(); // Force lazy loading
            }
            if (comment.user != null) {
                comment.user.username.length(); // Force lazy loading
            }
            comment.content.length(); // Force load content
        }
        return comments.stream()
                .map(PostCommentViewDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public PostCommentViewDto createComment(final PostCommentEntity comment, final String currentUsername) {
        // Validate content is provided for creation
        if (comment.content == null || comment.content.trim().isEmpty()) {
            throw new ValidationException("Content is required for creating a comment");
        }

        // Validate post exists
        final PostEntity existingPost = PostEntity.findById(comment.post != null ? comment.post.id : null);
        if (existingPost == null) {
            throw new WebApplicationException(
                    "Post with ID " + (comment.post != null ? comment.post.id : null) + " does not exist.",
                    Response.Status.BAD_REQUEST);
        }

        // Validate post is published
        if (existingPost.published == null || !existingPost.published) {
            throw new WebApplicationException("Cannot add comment to an unpublished post.",
                    Response.Status.BAD_REQUEST);
        }

        // Validate post allows comments
        if (existingPost.commentable == null || !existingPost.commentable) {
            throw new WebApplicationException("Comments are not allowed on this post.", Response.Status.BAD_REQUEST);
        }

        // Always assign the managed post entity
        comment.post = existingPost;

        // Auto-assign current user if not provided (skip existence check)
        if (comment.user == null) {
            comment.user = (UserEntity) UserEntity.find("username", currentUsername).firstResultOptional().orElse(null);
        }

        comment.created = LocalDateTime.now();
        comment.persist();

        // Force load lazy fields to avoid LazyInitializationException
        if (comment.post != null) {
            comment.post.title.length(); // Force lazy loading
        }
        if (comment.user != null) {
            comment.user.username.length(); // Force lazy loading
        }

        return new PostCommentViewDto(comment);
    }

    @Transactional
    public PostCommentViewDto updateComment(final PostCommentEntity comment) {
        final PostCommentEntity existingComment = PostCommentEntity.findById(comment.id);
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
        if (existingComment.post != null) {
            existingComment.post.title.length(); // Force lazy loading
        }
        if (existingComment.user != null) {
            existingComment.user.username.length(); // Force lazy loading
        }

        return new PostCommentViewDto(existingComment);
    }

    @Transactional
    public PostCommentViewDto patchComment(final PostCommentEntity comment) {
        final PostCommentEntity existingComment = PostCommentEntity.findById(comment.id);
        if (existingComment == null) {
            throw new WebApplicationException("Comment not found", Response.Status.NOT_FOUND);
        }

        // Partial update (PATCH semantics) - only update provided fields
        if (comment.content != null) {
            existingComment.content = comment.content;
        }

        existingComment.persist();

        // Force initialization of lazy fields to avoid LazyInitializationException
        if (existingComment.post != null) {
            existingComment.post.title.length(); // Force lazy loading
        }
        if (existingComment.user != null) {
            existingComment.user.username.length(); // Force lazy loading
        }

        return new PostCommentViewDto(existingComment);
    }

    @Transactional
    public boolean deleteComment(final Long id) {
        return PostCommentEntity.deleteById(id);
    }

    public List<PostCommentViewDto> searchComments(final String query) {
        if (query == null || query.trim().isEmpty()) {
            return this.getAllComments();
        }
        final var searchTerm = "%" + query.trim().toLowerCase() + "%";
        final List<PostCommentEntity> comments = PostCommentEntity.find(
                "content LIKE ?1 OR LOWER(user.username) LIKE ?1", searchTerm).list();

        // Force load lazy fields
        for (final PostCommentEntity comment : comments) {
            if (comment.post != null) {
                comment.post.title.length(); // Force lazy loading
            }
            if (comment.user != null) {
                comment.user.username.length(); // Force lazy loading
            }
            comment.content.length(); // Force load content
        }

        return comments.stream()
                .map(PostCommentViewDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<PostCommentViewDto> findByDateRange(final String startDate, final String endDate) {
        if (startDate == null || endDate == null) {
            return this.getAllComments();
        }

        try {
            final LocalDate start = LocalDate.parse(startDate);
            final LocalDate end = LocalDate.parse(endDate);

            final LocalDateTime startDateTime = start.atStartOfDay();
            final LocalDateTime endDateTime = end.atTime(LocalTime.MAX);

            final List<PostCommentEntity> comments = PostCommentEntity
                    .find("created >= ?1 AND created <= ?2", startDateTime, endDateTime).list();

            // Force load lazy fields
            for (final PostCommentEntity comment : comments) {
                if (comment.post != null) {
                    comment.post.title.length(); // Force lazy loading
                }
                if (comment.user != null) {
                    comment.user.username.length(); // Force lazy loading
                }
                comment.content.length(); // Force load content
            }

            return comments.stream()
                    .map(PostCommentViewDto::new)
                    .collect(Collectors.toList());
        } catch (final Exception e) {
            // If date parsing fails, return all comments
            return this.getAllComments();
        }
    }

    /**
     * Alias for findByDateRange - for backward compatibility with views
     */
    @Transactional
    public List<PostCommentViewDto> getCommentsByDateRange(final String startDate, final String endDate) {
        return this.findByDateRange(startDate, endDate);
    }

    /**
     * Alias for findByUserId - for backward compatibility with views
     */
    @Transactional
    public List<PostCommentViewDto> getCommentsByUser(final long userId) {
        return this.findByUserId(userId);
    }
}
